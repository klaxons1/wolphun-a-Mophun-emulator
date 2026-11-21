package wolphun.decomp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import wolphun.decomp.Program.Func;
import wolphun.decomp.Structurer.Node;

/**
 * PIP2 -> Java decompiler for Mophun (.mpn / VMGP) games.
 *
 * <pre>
 *   java -cp out wolphun.decomp.Main game.mpn outdir [--pkg=name] [--class=Name] [--addr] [--only=0xADDR]
 * </pre>
 * Writes outdir/[pkg/]Name.java: a class extending wolphun.decomp.rt.Pip2Runtime that can be
 * compiled against the emulator and run with wolphun.decomp.rt.RunDecompiled.
 *
 * Pipeline: Image (load/decrypt) -> Program (decode, switch tables, functions, CFGs) ->
 * Lifter (PIP2 -> IR) -> Analysis (calling convention, simplification) -> Structurer
 * (CFG -> structured AST) -> JavaWriter (AST -> Java source).
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        if(args.length<2){
            System.out.println("usage: wolphun.decomp.Main <game.mpn> <outdir> [--pkg=name] [--class=Name] [--addr] [--only=0xADDR]");
            System.out.println("  --addr   annotate every basic block with its bytecode address");
            System.out.println("  --only   decompile only the given function (for debugging)");
            System.exit(1);
        }
        File rom=new File(args[0]);
        File outDir=new File(args[1]);
        String pkg="", cls=null; boolean addr=false; Integer only=null;
        for(int i=2;i<args.length;i++){
            String a=args[i];
            if(a.startsWith("--pkg=")) pkg=a.substring(6);
            else if(a.startsWith("--class=")) cls=a.substring(8);
            else if(a.equals("--addr")) addr=true;
            else if(a.startsWith("--only=")) only=(int)Long.decode(a.substring(7)).longValue();
            else { System.err.println("unknown option "+a); System.exit(1); }
        }
        if(cls==null) cls=classNameFor(rom.getName());

        long t0=System.currentTimeMillis();
        Image img=Image.load(rom);
        Program prog=new Program(img);
        new Lifter(prog).liftAll();
        new Analysis(prog).run();

        Structurer st=new Structurer(prog, addr);
        LinkedHashMap<Func,Node> bodies=new LinkedHashMap<Func,Node>();
        List<String> notes=new ArrayList<String>();
        int failed=0;
        for(Func f : prog.funcs.values()){
            if(only!=null && f.entry!=only) continue;
            try {
                bodies.put(f, st.structure(f));
            } catch(RuntimeException e){
                failed++;
                notes.add(String.format("%s (0x%x): structuring failed: %s", f.name, f.entry, e));
                System.err.printf("structuring %s failed:%n", f.name);
                e.printStackTrace();
            }
        }
        String src=new JavaWriter(prog, cls, pkg).write(bodies, notes);

        File dir=pkg.isEmpty()? outDir : new File(outDir, pkg.replace('.', File.separatorChar));
        dir.mkdirs();
        File out=new File(dir, cls+".java");
        Files.write(out.toPath(), src.getBytes(StandardCharsets.UTF_8));

        int irr=0; for(Func f : bodies.keySet()) if(f.irreducible) irr++;
        System.out.printf("%s: %d functions (%d irreducible, %d failed), %d lines -> %s  [%d ms]%n",
            rom.getName(), bodies.size(), irr, failed, src.split("\n", -1).length, out, System.currentTimeMillis()-t0);
        for(String w : prog.warnings) System.out.println("  warning: "+w);
    }

    /** "HoneyCave2_[v1.0]_[multiscreen].mpn" -> "HoneyCave2" */
    static String classNameFor(String file){
        String s=file;
        int dot=s.indexOf('.'); if(dot>0) s=s.substring(0, dot);
        int br=s.indexOf('['); if(br>0) s=s.substring(0, br);
        StringBuilder b=new StringBuilder();
        boolean up=true;
        for(char c : s.toCharArray()){
            if(Character.isLetterOrDigit(c)){ b.append(up? Character.toUpperCase(c) : c); up=false; }
            else up=true;
        }
        if(b.length()==0 || !Character.isJavaIdentifierStart(b.charAt(0))) b.insert(0, "Game");
        return b.toString();
    }
}
