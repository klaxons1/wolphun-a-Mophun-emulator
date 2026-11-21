// Decompiled from HoneyCave2_[v1.0]_[multiscreen].mpn by the wolphun PIP2 -> Java decompiler (wolphun.decomp).
// Source: the PIP2 bytecode of a Mophun VMGP executable (decrypted first: the code section was encrypted).
//
// code  0x10000..0x17e98  (32408 bytes, 116 functions)
// data  0x17ea0..0x18cf8   bss 0x18d00..0x23b90
// SDK imports: 50   resources: 18
//
// How to read this file
//  * Registers are Java locals named after the PIP2 ABI: p0..p3 = arguments, r0 = result,
//    s0..s7/fp = callee-saved, g0..g13 = temporaries. sp is the emulated stack pointer.
//  * Memory is the game's own 32-bit address space: s8/u8/s16/u16/i32(addr) read,
//    put8/put16/put32(addr, value) write. g_XXXXX are data/bss addresses, fun_XXXXX code.
//  * ltu/leu/gtu/geu are unsigned comparisons; setB/setH replace the low byte/half of a value
//    (PIP2 .b/.h register operations).
//  * The class compiles against desktop/out and runs with wolphun.decomp.rt.RunDecompiled.

import wolphun.decomp.rt.Pip2Runtime;

@SuppressWarnings("unused")
public final class HoneyCave2 extends Pip2Runtime {
    
    // ---- address-taken functions (function pointers; dispatched by icall)
    static final int fun_118b4 = 0x118b4;
    static final int fun_118ec = 0x118ec;
    static final int fun_11e9c = 0x11e9c;
    static final int fun_11eac = 0x11eac;
    static final int fun_12164 = 0x12164;
    static final int fun_121dc = 0x121dc;
    static final int fun_12298 = 0x12298;
    static final int fun_12514 = 0x12514;
    static final int fun_125ec = 0x125ec;
    static final int fun_128a0 = 0x128a0;
    static final int fun_12d50 = 0x12d50;
    static final int fun_12dac = 0x12dac;
    static final int fun_13220 = 0x13220;
    static final int fun_14144 = 0x14144;
    static final int fun_14834 = 0x14834;
    static final int fun_14920 = 0x14920;
    static final int fun_14a74 = 0x14a74;
    static final int fun_14b3c = 0x14b3c;
    static final int fun_14c3c = 0x14c3c;
    static final int fun_14d68 = 0x14d68;
    static final int fun_14e50 = 0x14e50;
    static final int fun_14f94 = 0x14f94;
    static final int fun_152a4 = 0x152a4;
    static final int fun_15370 = 0x15370;
    static final int fun_15450 = 0x15450;
    static final int fun_157e0 = 0x157e0;
    static final int fun_158c0 = 0x158c0;
    static final int fun_15c78 = 0x15c78;
    static final int fun_15ecc = 0x15ecc;
    static final int fun_16040 = 0x16040;
    static final int fun_16224 = 0x16224;
    static final int fun_16364 = 0x16364;
    static final int fun_16460 = 0x16460;
    static final int fun_164f0 = 0x164f0;
    static final int fun_16bbc = 0x16bbc;
    static final int fun_16cd8 = 0x16cd8;
    static final int fun_16d78 = 0x16d78;
    static final int fun_16e18 = 0x16e18;
    
    // ---- data / bss addresses referenced by the code
    static final int g_17ea0 = 0x17ea0;   // data  "MThd"
    static final int g_17f43 = 0x17f43;   // data  "MThd"
    static final int g_17f93 = 0x17f93;   // data  "MThd"
    static final int g_17fe7 = 0x17fe7;   // data  "MThd"
    static final int g_18038 = 0x18038;   // data  "MThd"
    static final int g_180bc = 0x180bc;   // data  "MThd"
    static final int g_181dc = 0x181dc;   // data  "MThd"
    static final int g_18270 = 0x18270;   // data  "MThd"
    static final int g_18330 = 0x18330;   // data
    static final int g_183e0 = 0x183e0;   // data
    static final int g_18470 = 0x18470;   // data
    static final int g_18474 = 0x18474;   // data
    static final int g_18478 = 0x18478;   // data
    static final int g_1847c = 0x1847c;   // data  u" < "
    static final int g_1847e = 0x1847e;   // data  u"< "
    static final int g_18480 = 0x18480;   // data
    static final int g_18482 = 0x18482;   // data
    static final int g_184a6 = 0x184a6;   // data  u" >"
    static final int g_184ac = 0x184ac;   // data  u";:楨桧捳牯e"
    static final int g_184ae = 0x184ae;   // data  u":楨桧捳牯e"
    static final int g_184b0 = 0x184b0;   // data  "highscore"
    static final int g_184bc = 0x184bc;   // data
    static final int g_184fc = 0x184fc;   // data
    static final int g_18508 = 0x18508;   // data
    static final int g_1850c = 0x1850c;   // data
    static final int g_18520 = 0x18520;   // data
    static final int g_18524 = 0x18524;   // data
    static final int g_18528 = 0x18528;   // data
    static final int g_1852c = 0x1852c;   // data
    static final int g_18530 = 0x18530;   // data
    static final int g_18538 = 0x18538;   // data
    static final int g_1854c = 0x1854c;   // data
    static final int g_18574 = 0x18574;   // data
    static final int g_1857c = 0x1857c;   // data
    static final int g_18584 = 0x18584;   // data
    static final int g_1858c = 0x1858c;   // data
    static final int g_18590 = 0x18590;   // data
    static final int g_18594 = 0x18594;   // data
    static final int g_1859c = 0x1859c;   // data
    static final int g_185a4 = 0x185a4;   // data
    static final int g_185b0 = 0x185b0;   // data
    static final int g_185bc = 0x185bc;   // data
    static final int g_185c8 = 0x185c8;   // data
    static final int g_185d8 = 0x185d8;   // data
    static final int g_185e8 = 0x185e8;   // data
    static final int g_185f8 = 0x185f8;   // data
    static final int g_18600 = 0x18600;   // data
    static final int g_1860c = 0x1860c;   // data
    static final int g_18617 = 0x18617;   // data
    static final int g_18648 = 0x18648;   // data
    static final int g_1864c = 0x1864c;   // data
    static final int g_18674 = 0x18674;   // data
    static final int g_186c4 = 0x186c4;   // data
    static final int g_186f4 = 0x186f4;   // data
    static final int g_186f6 = 0x186f6;   // data
    static final int g_186f8 = 0x186f8;   // data
    static final int g_186fa = 0x186fa;   // data
    static final int g_186fc = 0x186fc;   // data
    static final int g_186fe = 0x186fe;   // data
    static final int g_186ff = 0x186ff;   // data
    static final int g_18700 = 0x18700;   // data
    static final int g_18701 = 0x18701;   // data
    static final int g_18702 = 0x18702;   // data
    static final int g_18703 = 0x18703;   // data
    static final int g_18706 = 0x18706;   // data
    static final int g_18708 = 0x18708;   // data
    static final int g_1870a = 0x1870a;   // data
    static final int g_1870c = 0x1870c;   // data
    static final int g_1870e = 0x1870e;   // data
    static final int g_18710 = 0x18710;   // data
    static final int g_18712 = 0x18712;   // data
    static final int g_18713 = 0x18713;   // data
    static final int g_18715 = 0x18715;   // data
    static final int g_18716 = 0x18716;   // data
    static final int g_18717 = 0x18717;   // data
    static final int g_18718 = 0x18718;   // data
    static final int g_1871a = 0x1871a;   // data
    static final int g_1871c = 0x1871c;   // data
    static final int g_1871e = 0x1871e;   // data
    static final int g_18720 = 0x18720;   // data
    static final int g_18722 = 0x18722;   // data
    static final int g_18724 = 0x18724;   // data
    static final int g_18728 = 0x18728;   // data
    static final int g_1872c = 0x1872c;   // data
    static final int g_1872e = 0x1872e;   // data
    static final int g_18730 = 0x18730;   // data
    static final int g_18738 = 0x18738;   // data
    static final int g_18a24 = 0x18a24;   // data
    static final int g_18a28 = 0x18a28;   // data
    static final int g_18a29 = 0x18a29;   // data
    static final int g_18a2b = 0x18a2b;   // data
    static final int g_18a34 = 0x18a34;   // data
    static final int g_18a38 = 0x18a38;   // data
    static final int g_18a3c = 0x18a3c;   // data
    static final int g_18a40 = 0x18a40;   // data
    static final int g_18a43 = 0x18a43;   // data
    static final int g_18a46 = 0x18a46;   // data
    static final int g_18a48 = 0x18a48;   // data
    static final int g_18a50 = 0x18a50;   // data
    static final int g_18bbc = 0x18bbc;   // data
    static final int g_18bc8 = 0x18bc8;   // data
    static final int g_18bca = 0x18bca;   // data
    static final int g_18bd4 = 0x18bd4;   // data
    static final int g_18bec = 0x18bec;   // data
    static final int g_18bee = 0x18bee;   // data
    static final int g_18bf0 = 0x18bf0;   // data
    static final int g_18bf2 = 0x18bf2;   // data
    static final int g_18bf4 = 0x18bf4;   // data
    static final int g_18bf6 = 0x18bf6;   // data
    static final int g_18bf8 = 0x18bf8;   // data
    static final int g_18bfa = 0x18bfa;   // data
    static final int g_18bfc = 0x18bfc;   // data
    static final int g_18c00 = 0x18c00;   // data
    static final int g_18c04 = 0x18c04;   // data
    static final int g_18c05 = 0x18c05;   // data
    static final int g_18c06 = 0x18c06;   // data
    static final int g_18c07 = 0x18c07;   // data
    static final int g_18c08 = 0x18c08;   // data
    static final int g_18c53 = 0x18c53;   // data  u"爙獥浵e儀礀最吀礀夀"
    static final int g_18c54 = 0x18c54;   // data  "resume"
    static final int g_18c5c = 0x18c5c;   // data  u"QygTyY"
    static final int g_18cc4 = 0x18cc4;   // data
    static final int g_18cdc = 0x18cdc;   // data  "language"
    static final int g_18ce5 = 0x18ce5;   // data
    static final int g_18d00 = 0x18d00;   // bss
    static final int g_18d02 = 0x18d02;   // bss
    static final int g_18d04 = 0x18d04;   // bss
    static final int g_18d08 = 0x18d08;   // bss
    static final int g_18d0c = 0x18d0c;   // bss
    static final int g_18d0e = 0x18d0e;   // bss
    static final int g_18d10 = 0x18d10;   // bss
    static final int g_18d12 = 0x18d12;   // bss
    static final int g_18d14 = 0x18d14;   // bss
    static final int g_18d16 = 0x18d16;   // bss
    static final int g_18d18 = 0x18d18;   // bss
    static final int g_18d1a = 0x18d1a;   // bss
    static final int g_18d1c = 0x18d1c;   // bss
    static final int g_18d20 = 0x18d20;   // bss
    static final int g_18d22 = 0x18d22;   // bss
    static final int g_18d24 = 0x18d24;   // bss
    static final int g_18d26 = 0x18d26;   // bss
    static final int g_18d28 = 0x18d28;   // bss
    static final int g_18d2a = 0x18d2a;   // bss
    static final int g_18d2c = 0x18d2c;   // bss
    static final int g_18d2e = 0x18d2e;   // bss
    static final int g_18d30 = 0x18d30;   // bss
    static final int g_18d32 = 0x18d32;   // bss
    static final int g_18d34 = 0x18d34;   // bss
    static final int g_18d38 = 0x18d38;   // bss
    static final int g_18d40 = 0x18d40;   // bss
    static final int g_18d44 = 0x18d44;   // bss
    static final int g_18d48 = 0x18d48;   // bss
    static final int g_18d4c = 0x18d4c;   // bss
    static final int g_18d90 = 0x18d90;   // bss
    static final int g_18d94 = 0x18d94;   // bss
    static final int g_18d98 = 0x18d98;   // bss
    static final int g_18d9c = 0x18d9c;   // bss
    static final int g_18da0 = 0x18da0;   // bss
    static final int g_18da4 = 0x18da4;   // bss
    static final int g_18da8 = 0x18da8;   // bss
    static final int g_18db0 = 0x18db0;   // bss
    static final int g_18dbc = 0x18dbc;   // bss
    static final int g_18dc0 = 0x18dc0;   // bss
    static final int g_18dc4 = 0x18dc4;   // bss
    static final int g_18dd0 = 0x18dd0;   // bss
    static final int g_18dd8 = 0x18dd8;   // bss
    static final int g_18de4 = 0x18de4;   // bss
    static final int g_18de8 = 0x18de8;   // bss
    static final int g_18df2 = 0x18df2;   // bss
    static final int g_18dfc = 0x18dfc;   // bss
    static final int g_18e00 = 0x18e00;   // bss
    static final int g_18e10 = 0x18e10;   // bss
    static final int g_19100 = 0x19100;   // bss
    static final int g_19104 = 0x19104;   // bss
    static final int g_19108 = 0x19108;   // bss
    static final int g_1910c = 0x1910c;   // bss
    static final int g_19110 = 0x19110;   // bss
    static final int g_19114 = 0x19114;   // bss
    static final int g_19118 = 0x19118;   // bss
    static final int g_1911c = 0x1911c;   // bss
    static final int g_19120 = 0x19120;   // bss
    static final int g_19124 = 0x19124;   // bss
    static final int g_19128 = 0x19128;   // bss
    static final int g_19130 = 0x19130;   // bss
    static final int g_1bef4 = 0x1bef4;   // bss
    static final int g_1bef7 = 0x1bef7;   // bss
    static final int g_22d70 = 0x22d70;   // bss
    static final int g_22d72 = 0x22d72;   // bss
    static final int g_22df0 = 0x22df0;   // bss
    static final int g_22df4 = 0x22df4;   // bss
    static final int g_22e00 = 0x22e00;   // bss
    static final int g_22e04 = 0x22e04;   // bss
    static final int g_22e08 = 0x22e08;   // bss
    static final int g_22e0c = 0x22e0c;   // bss
    static final int g_22e10 = 0x22e10;   // bss
    static final int g_22e12 = 0x22e12;   // bss
    static final int g_23210 = 0x23210;   // bss
    static final int g_23610 = 0x23610;   // bss
    static final int g_23650 = 0x23650;   // bss
    static final int g_23690 = 0x23690;   // bss
    static final int g_236d0 = 0x236d0;   // bss
    static final int g_23710 = 0x23710;   // bss
    static final int g_23750 = 0x23750;   // bss
    static final int g_23850 = 0x23850;   // bss
    static final int g_23890 = 0x23890;   // bss
    static final int g_238d0 = 0x238d0;   // bss
    static final int g_239d0 = 0x239d0;   // bss
    static final int g_23a10 = 0x23a10;   // bss
    static final int g_23a90 = 0x23a90;   // bss
    static final int g_23ad0 = 0x23ad0;   // bss
    static final int g_23b10 = 0x23b10;   // bss
    
    @Override public void entry() { start(); }
    
    /** indirect calls (CALLr) and constructors: function pointer -> method */
    @Override protected int icall(int fn, int p0, int p1, int p2, int p3) {
        switch (fn) {
            case fun_118b4: fun_118b4(); return 0;
            case fun_118ec: return fun_118ec();
            case fun_11e9c: fun_11e9c(); return 0;
            case fun_11eac: return fun_11eac();
            case fun_12164: return fun_12164();
            case fun_121dc: return fun_121dc();
            case fun_12298: return fun_12298();
            case fun_12514: return fun_12514(p0);
            case fun_125ec: return fun_125ec();
            case fun_128a0: return fun_128a0();
            case fun_12d50: return fun_12d50();
            case fun_12dac: fun_12dac(); return 0;
            case fun_13220: return fun_13220();
            case fun_14144: return fun_14144();
            case fun_14834: return fun_14834(p0);
            case fun_14920: return fun_14920(p0);
            case fun_14a74: return fun_14a74(p0);
            case fun_14b3c: return fun_14b3c(p0);
            case fun_14c3c: return fun_14c3c(p0);
            case fun_14d68: return fun_14d68(p0);
            case fun_14e50: return fun_14e50(p0);
            case fun_14f94: return fun_14f94(p0);
            case fun_152a4: return fun_152a4(p0);
            case fun_15370: return fun_15370(p0);
            case fun_15450: return fun_15450(p0);
            case fun_157e0: return fun_157e0(p0);
            case fun_158c0: return fun_158c0(p0);
            case fun_15c78: return fun_15c78(p0);
            case fun_15ecc: return fun_15ecc(p0);
            case fun_16040: return fun_16040(p0);
            case fun_16224: return fun_16224(p0);
            case fun_16364: return fun_16364(p0);
            case fun_16460: return fun_16460(p0);
            case fun_164f0: return fun_164f0(p0);
            case fun_16bbc: return fun_16bbc();
            case fun_16cd8: return fun_16cd8();
            case fun_16d78: return fun_16d78();
            case fun_16e18: return fun_16e18();
            default: throw badPointer(fn);
        }
    }
    
    /**
     * start @ 0x10000
     */
    void start() {
        main();
        vTerminateVMGP();
        throw terminated();
    }
    
    /**
     * fun_10010 @ 0x10010
     * called from: fun_1022c, fun_104f4, fun_105fc, fun_10768, fun_10f68, fun_1100c, fun_11eac, fun_12dd4
     */
    void fun_10010(int p0) {
        int p2 = 0;
        B_1004c: {
            sp -= 4;
            p2 = p0 & 0xFFFF;
            if (u16(g_186f4) > 128) {
                if (u16(g_186f6) > 127) {
                    vSelectFont(0, 0x8000000, u16(g_23ad0));
                    break B_1004c;
                }
            }
            vSelectFont(1, 0x8000000, p2);
        }
        sp += 4;
    }
    
    /**
     * main @ 0x10074
     * called from: start
     */
    void main() {
        int fp = 0, p0 = 0, r0 = 0;
        sp -= 12;
        if ((fun_12dd4() & 0xFF) != 0) {
            vTerminateVMGP();
            throw terminated();
        }
        fp = vGetTickCount();
        L_10090: while (true) {
            B_100d8: {
                put16(g_18da0, vGetRandom());
                p0 = u8(g_186ff);
                put8(g_186ff, p0 + 1);
                if (u8(g_18da8) == 0) {
                    r0 = 1;
                } else {
                    r0 = u8(g_18701);
                    if ((r0 & 0xFF) == 0) {
                        break B_100d8;
                    }
                    r0 = (r0 & 0xFF) + 255;
                }
                put8(g_18701, r0);
            }
            B_10150: {
                r0 = vGetButtonData();
                put32(g_19118, r0);
                if ((r0 & 0x20) != 0) {
                    while (true) {
                        if ((vGetButtonData() & 0xFFFF) == 0) {
                            p0 = fun_11eac;
                            if (i32(g_19128) == p0) {
                                break B_10150;
                            }
                            if (u8(g_186fc) == 0) {
                                break B_10150;
                            }
                            fun_118ec();
                            put8(g_19100, 1);
                            p0 = fun_11eac;
                            put32(g_19128, p0);
                            break B_10150;
                        }
                    }
                }
            }
            icall(i32(g_19128), p0);
            vFlipScreen(0);
            while (true) {
                if (leu(fp, vGetTickCount())) {
                    fp = vGetTickCount() + 75;
                    continue L_10090;
                }
            }
        }
    }
    
    /**
     * fun_101a0 @ 0x101a0
     * called from: fun_10a54, fun_1123c
     */
    void fun_101a0() {
        int p0 = 0, p1 = 0, r0 = 0, r1 = 0;
        B_101dc: {
            sp -= 4;
            p0 = g_18d32;
            if (u16(p0) == 0) {
                p1 = g_18d34;
                r1 = u16(p1);
                if ((short) r1 > 6) {
                    put16(p0, 1);
                    break B_101dc;
                }
                r0 = (r1 & 0xFFFF) + 1;
            } else {
                p1 = g_18d34;
                r1 = u16(p1);
                if ((short) r1 <= 0) {
                    put16(p0, 0);
                    break B_101dc;
                }
                r0 = (r1 & 0xFFFF) - 1;
            }
            put16(p1, r0);
        }
        sp += 4;
    }
    
    /**
     * fun_1022c @ 0x1022c
     * called from: fun_11eac, fun_12dd4
     */
    int fun_1022c(int p0, int p1, int p2, int p3) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, r0 = 0;
        sp -= 24;
        s2 = p0 & 0xFFFF;
        fp = p1 & 0xFFFF;
        s3 = p2 & 0xFFFF;
        s0 = p3 & 0xFFFF;
        s1 = ((vTextExtentU(g_236d0) >>> 16) & 0xFFFF) + 2;
        fun_10010(65);
        r0 = ((vCharExtent(65) >>> 16) & 0xFFFF) + 1;
        put16(g_18d30, r0);
        p2 = r0 & 0xFFFF;
        p3 = s1 & 0xFFFF;
        p1 = s0 & 0xFFFF;
        if (((p2 * 3 + p3) << 1) <= p1 - 3) {
            put16(g_18d2c, 6);
            r0 = p2 * 6;
        } else {
            r0 = div(p1 - (p3 << 1) - 3, p2);
            put16(g_18d2c, r0);
            r0 = (r0 & 0xFFFF) * p2;
        }
        r0 = p1 - r0;
        put16(g_18d28, fp + ((r0 + (r0 >>> 31)) >> 1));
        put16(g_18d2e, fp + s0 - s1);
        put16(g_18d20, s2);
        put16(g_18d22, fp);
        put16(g_18d24, s3);
        put16(g_18d26, s0);
        put32(g_18d38, i32(sp + 24));
        r0 = 0;
        put16(g_18d2a, r0);
        put16(g_18d32, r0);
        put16(g_18d34, r0);
        put32(g_18470, g_23890);
        put32(g_18474, g_23850);
        r0 = g_238d0;
        put32(g_18478, r0);
        sp += 24;
        return r0;
    }
    
    /**
     * fun_103a8 @ 0x103a8
     * called from: fun_1048c
     */
    void fun_103a8(int p0, int p1, int p2, int p3) {
        int g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, g5 = 0, g6 = 0;
        sp -= 4;
        g2 = p0 & 0xFFFF;
        g4 = p1 & 0xFFFF;
        g3 = p3;
        g5 = i32(sp + 4);
        g6 = i32(sp + 8);
        if (g5 < g3) {
            g0 = g5 * p2;
            vSetForeColor(0x80001DEF);
            g1 = (g4 & 0xFFFF) + (g0 & 0xFFFF);
            vFillRect(g2, p1, g2 + 6, g1 + 1);
            vSetForeColor(0x8000210A);
            vFillRect(g2 + 1, g4 + 1, g2 + 5, g1);
            g0 = (g0 & 0xFFFF) - 2;
            p3 = div(g0 * g5, g3);
            if (g6 == g3 - g5) {
                p1 = ((g1 - p3) & 0xFFFF) - 1;
            } else {
                p1 = ((g4 + div(g0 * g6, g3)) & 0xFFFF) + 2;
            }
            vSetForeColor(0x80007FFF);
            vFillRect(g2 + 2, p1, g2 + 4, p1 + p3);
        }
        sp += 4;
    }
    
    /**
     * fun_1048c @ 0x1048c
     * called from: fun_10a54
     */
    void fun_1048c() {
        int p0 = 0, p1 = 0, p2 = 0;
        sp -= 12;
        p0 = ((u16(g_18d20) + u16(g_18d24)) & 0xFFFF) - 8;
        p1 = u16(g_18d28) - 1;
        p2 = u16(g_18d30);
        put32(sp, u16(g_18d2c));
        put32(sp + 4, u16(g_18d2a));
        fun_103a8(p0, p1, p2, 6);
        sp += 12;
    }
    
    /**
     * fun_104f4 @ 0x104f4
     * called from: fun_10a54, fun_1123c
     */
    void fun_104f4() {
        int s0 = 0, p1 = 0;
        sp -= 12;
        vSetForeColor(0x800003FF);
        s0 = g_236d0;
        fun_10010(u16(s0));
        p1 = u16(g_18d24) - (vTextExtentU(s0) & 0xFFFF);
        vTextOutU(((p1 + (p1 >>> 31)) >> 1) + u16(g_18d20), u16(g_18d22), s0);
        sp += 12;
    }
    
    /**
     * fun_10574 @ 0x10574
     * called from: fun_1123c
     */
    void fun_10574(int p0) {
        int p1 = 0, p3 = 0;
        sp -= 12;
        p3 = g_18d30;
        p1 = ((u16(p3) * ((byte) p0 - u16(g_18d2a))) & 0xFFFF) + u16(g_18d28);
        p0 = u16(g_18d20);
        vSetClipWindow(p0, p1, p0 + u16(g_18d24), p1 + u16(p3) - 2);
        icall(i32(g_18d38), p0);
        vSetClipWindow(0, 0, 128, 160);
        sp += 12;
    }
    
    /**
     * fun_105fc @ 0x105fc
     * called from: fun_10a54, fun_1123c
     */
    void fun_105fc(int p0) {
        int s0 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, g5 = 0;
        sp -= 20;
        s0 = p0;
        fun_10010(65);
        g3 = vTextExtent(g_1847c) /* u" < " */;
        g4 = g_18d20;
        g5 = u16(g4) + (g3 & 0xFFFF);
        g1 = ((u16(g_18d30) * (s0 - u16(g_18d2a))) & 0xFFFF) + u16(g_18d28);
        g0 = sp;
        p3 = 48;
        vitoa(s0 + 1, g0, 1, p3);
        p1 = g1 & 0xFFFF;
        vTextOut(g5, p1, g0);
        g5 = (g5 & 0xFFFF) + ((vTextExtent(g0) + g3) & 0xFFFF);
        s0 *= 12;
        g2 = g_18d40;
        p2 = s0 + g2;
        vTextOut(g5, p1, p2);
        vitoa(i32(p2 + 4), g0, 6, p3);
        g5 = ((u16(g4) + u16(g_18d24) - vTextExtent(g0) - g3) & 0xFFFF) - 7;
        vTextOut(g5, g1, g0);
        vitoa(u8(s0 + g2 + 8), g0, 2, p3);
        p0 = (g5 - (vTextExtent(g0) + g3)) & 0xFFFF;
        vTextOut(p0, g1, g0);
        sp += 20;
    }
    
    /**
     * fun_10768 @ 0x10768
     * called from: fun_10a54
     */
    void fun_10768(int p0) {
        int s1 = 0, s2 = 0, s3 = 0, s4 = 0, p1 = 0;
        sp -= 68;
        put16(sp, u16(g_1847e));
        put16(sp + 2, u16(g_18480));
        put16(sp + 4, u16(g_18482));
        memset(sp + 6, 0, 34);
        s1 = sp;
        fun_1784c(s1, (p0 << 6) + g_23610);
        fun_1784c(s1, g_184a6);
        s3 = g_18d20;
        s4 = g_18d2e;
        s2 = g_18d24;
        p0 = u16(s3);
        vSetClipWindow(p0, u16(s4), p0 + u16(s2) - 1, u16(g_18d22) + u16(g_18d26));
        icall(i32(g_18d38), p0);
        vSetForeColor(0x800003FF);
        fun_10010(u16(g_236d0));
        p1 = (u16(s2) - (vTextExtentU(s1) & 0xFFFF)) >> 1;
        vTextOutU(p1 + u16(s3), u16(s4), s1);
        vSetClipWindow(0, 0, 128, 160);
        sp += 68;
    }
    
    /**
     * fun_108c8 @ 0x108c8
     * called from: fun_10a54
     */
    int fun_108c8() {
        int r0 = 0;
        B_108fc: {
            sp -= 16;
            put16(sp, 12);
            vGetCaps(4, sp);
            if (u16(sp + 4) == 3) {
                r0 = 1;
                if ((i32(sp + 8) & 4) != 0) {
                    break B_108fc;
                }
            }
            r0 = 0;
        }
        sp += 16;
        return r0;
    }
    
    /**
     * fun_10924 @ 0x10924
     * called from: fun_10a54
     */
    void fun_10924() {
        int p0 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, r0 = 0;
        sp -= 44;
        put8(sp, 0);
        g4 = 0;
        while (true) {
            g3 = sp;
            r0 = g3 + vStrLen(g3);
            g0 = g4 * 12;
            g1 = g_18d40;
            vStrCpy(r0, g0 + g1);
            r0 = g3 + vStrLen(g3);
            g2 = g_184ac;
            vStrCpy(r0, g2);
            p1 = g3 + vStrLen(g3);
            p2 = 0;
            p3 = p2 & 0xFF;
            vitoa(u8(g0 + g1 + 8), p1, p2, p3);
            p0 = g3 + vStrLen(g3);
            vStrCpy(p0, g2);
            p1 = g3 + vStrLen(g3);
            vitoa(i32(g0 + g1 + 4), p1, p2, p3);
            p0 = g3 + vStrLen(g3);
            vStrCpy(p0, g_184ae) /* u":楨桧捳牯e" */;
            g4++;
            if (g4 > 2) {
                vSysCtl(0x80000003, g3);
                sp += 44;
                return;
            }
        }
    }
    
    /**
     * fun_10a54 @ 0x10a54
     * called from: fun_11eac, fun_128a0
     */
    int fun_10a54(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, s7 = 0, p1 = 0, p2 = 0, r0 = 0;
        B_10ab0: {
            sp -= 52;
            s3 = p0 & 0xFFFF;
            s4 = vGetTickCount();
            s6 = s4;
            p0 = (short) s3;
            if (p0 <= 5) {
                p1 = g_18d2c;
                if (p0 >= u16(p1)) {
                    put16(g_18d2a, s3 - u16(p1) + 1);
                    break B_10ab0;
                }
            }
            p0 = 0;
            put16(g_18d2a, p0);
        }
        s2 = 3 - ((((fun_108c8() & 0xFF) - 1) >>> 31) & 0xFFFF);
        L_10ad0: while (true) {
            icall(i32(g_18d38), p0);
            fun_104f4();
            fp = g_18d2a;
            s0 = u16(fp);
            p1 = s0 & 0xFFFF;
            r0 = p1;
            s1 = g_18d2c;
            while (true) {
                if (p1 >= r0 + u16(s1)) {
                    fp = 0;
                    put32(sp, 12);
                    put32(sp + 4, 13);
                    put32(sp + 8, 14);
                    fun_10768(12);
                    fun_1048c();
                    vFlipScreen(0);
                    s5 = 1;
                    s1 = 500;
                    s7 = 0;
                    while (true) {
                        B_10c54: {
                            B_10e7c: {
                                p1 = vGetButtonData();
                                if (p1 != 0) {
                                    if ((s5 & 0xFF) != 0) {
                                        if (geu(s6 + s1, vGetTickCount())) {
                                            break B_10c54;
                                        }
                                        s1 = 70;
                                    } else {
                                        s1 = 500;
                                    }
                                    s5 = 1;
                                    s6 = vGetTickCount();
                                    if ((p1 & 4) == 0) {
                                        if ((p1 & 8) == 0) {
                                            if ((p1 & 1) != 0) {
                                                p1 = g_18d2a;
                                                p0 = u16(p1);
                                                if ((p0 & 0xFFFF) == 0) {
                                                    p0 = u16(g_18d2c);
                                                    put16(p1, 6 - p0);
                                                    continue L_10ad0;
                                                }
                                                put16(p1, p0 - 1);
                                                continue L_10ad0;
                                            }
                                            if ((p1 & 2) != 0) {
                                                p2 = g_18d2a;
                                                p0 = u16(g_18d2c);
                                                if (u16(p2) == 6 - p0) {
                                                    put16(p2, 0);
                                                    continue L_10ad0;
                                                }
                                                p0 = u16(p2);
                                                put16(p2, p0 + 1);
                                                continue L_10ad0;
                                            }
                                            if ((p1 & 0x10) == 0) {
                                                r0 = p1 & 0x20;
                                                if (r0 == 0) {
                                                    break B_10e7c;
                                                }
                                            } else {
                                                r0 = fp & 0xFFFF;
                                                if (r0 == 1) {
                                                    if (vMsgBoxU(66, g_23610) != 1) {
                                                        break B_10c54;
                                                    }
                                                    fun_10f50();
                                                    fun_10ea0();
                                                    s7 = 1;
                                                    s3 = -1;
                                                    break B_10c54;
                                                }
                                                if (r0 == 2) {
                                                    fun_10924();
                                                    break B_10c54;
                                                }
                                            }
                                            sp += 52;
                                            return r0;
                                        }
                                        p0 = (fp & 0xFFFF) + 1;
                                        p1 = s2 & 0xFFFF;
                                        fp = p0 - div(p0, p1) * p1;
                                    } else {
                                        p0 = (fp & 0xFFFF) - 1;
                                        if ((fp & 0xFFFF) == 0) {
                                            p0 = (s2 & 0xFFFF) - 1;
                                        }
                                        fp = p0 & 0xFFFF;
                                    }
                                    fun_10768(i32(sp + 12 + ((fp & 0xFFFF) << 2) - 12));
                                    fun_1048c();
                                    vFlipScreen(0);
                                    break B_10c54;
                                }
                            }
                            s1 = 500;
                            s5 = 0;
                        }
                        B_10cd8: {
                            s0 = (short) s3;
                            p0 = u16(g_18d2a);
                            if (s0 >= p0) {
                                if (s0 < p0 + u16(g_18d2c)) {
                                    p0 = s4 + 50;
                                    if (ltu(p0, vGetTickCount())) {
                                        vSetForeColor(i32((s16(g_18d34) << 2) + g_18330));
                                        s4 = vGetTickCount();
                                        fun_101a0();
                                        fun_105fc(s0);
                                        p0 = 0;
                                        vFlipScreen(p0);
                                        break B_10cd8;
                                    }
                                }
                            }
                            sleep();
                        }
                        if ((s7 & 0xFF) != 0) {
                            continue L_10ad0;
                        }
                    }
                }
                vSetForeColor(0x80007FFF);
                fun_105fc(s0 & 0xFFFF);
                s0 = (s0 & 0xFFFF) + 1;
                p1 = s0 & 0xFFFF;
                r0 = u16(fp);
            }
        }
    }
    
    /**
     * fun_10ea0 @ 0x10ea0
     * called from: fun_10a54, fun_128a0
     */
    void fun_10ea0() {
        int p0 = 0, r0 = 0;
        B_10ee0: {
            sp -= 4;
            p0 = g_184b0;
            r0 = vStreamOpen(p0, 1536);
            if (r0 == -1) {
                r0 = vStreamOpen(p0, 3584);
                if (r0 == -1) {
                    break B_10ee0;
                }
            }
            p0 = r0;
            vStreamWrite(p0, g_18d40, 72);
            vStreamClose(p0);
        }
        sp += 4;
    }
    
    /**
     * fun_10f00 @ 0x10f00
     * called from: fun_12dd4
     */
    void fun_10f00() {
        int p0 = 0;
        sp -= 4;
        p0 = vStreamOpen(g_184b0, 256) /* "highscore" */;
        if (p0 != -1) {
            vStreamRead(p0, g_18d40, 72);
            vStreamClose(p0);
        }
        sp += 4;
    }
    
    /**
     * fun_10f50 @ 0x10f50
     * called from: fun_10a54
     */
    void fun_10f50() {
        sp -= 4;
        memset(g_18d40, 0, 72);
        sp += 4;
    }
    
    /**
     * fun_10f68 @ 0x10f68
     * called from: fun_1123c
     */
    void fun_10f68(int p0) {
        int s0 = 0, p1 = 0, p2 = 0, r0 = 0;
        sp -= 12;
        fun_10010(u16(g_236d0));
        s0 = ((p0 & 0xFFFF) << 2) + g_183e0;
        r0 = vTextExtentU(i32(s0)) & 0xFFFF;
        p2 = u16(g_18d24) - r0;
        p2 = (((p2 + (p2 >>> 31)) >> 1) & 0xFFFF) + u16(g_18d20);
        p1 = u16(g_18d2e);
        vSetForeColor(i32((s16(g_18d34) << 2) + g_18330));
        vTextOutU(p2, p1, i32(s0));
        sp += 12;
    }
    
    /**
     * fun_1100c @ 0x1100c
     * called from: fun_1123c
     */
    void fun_1100c(int p0) {
        int fp = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, s7 = 0, p2 = 0, p3 = 0, g0 = 0, r0 = 0;
        sp -= 40;
        s4 = p0 & 0xFFFF;
        fun_10010(u16(g_236d0));
        s7 = s4 & 0xFFFF;
        s2 = (s7 << 2) + g_183e0;
        s1 = vTextExtentU(i32(s2));
        s5 = g_18d24;
        s6 = s1 & 0xFFFF;
        r0 = u16(s5) - s6;
        p0 = u16(g_18d20);
        fp = (p0 & 0xFFFF) + (((r0 + (r0 >>> 31)) >> 1) & 0xFFFF);
        s3 = u16(g_18d2e);
        vSetClipWindow(p0, s3 - 2, p0 + u16(s5) - 1, u16(g_18d22) + u16(g_18d26));
        icall(i32(g_18d38), p0);
        vSetForeColor(0x80007FEF);
        vTextOutU(fp, s3, i32(s2));
        fp = ((fp + s1) & 0xFFFF) + 5;
        g0 = s4 & 0xFFFF;
        while (true) {
            p0 = (g0 & 0xFFFF) + 1;
            g0 = p0 - p0 / 39 * 39;
            p3 = ((g0 & 0xFFFF) << 2) + g_183e0;
            vTextOutU(fp, s3, i32(p3));
            fp = ((vTextExtentU(i32(p3)) + fp) & 0xFFFF) + 5;
            if ((short) fp >= u16(g_18d20) + u16(s5)) {
                g0 = (s4 & 0xFFFF) - 1;
                if (s7 == 0) {
                    g0 = 38;
                }
                r0 = u16(s5) - s6;
                fp = (((r0 + (r0 >>> 31)) >> 1) & 0xFFFF) + u16(g_18d20);
                while (true) {
                    p3 = g0 & 0xFFFF;
                    p2 = (p3 << 2) + g_183e0;
                    fp = ((fp - vTextExtentU(i32(p2))) & 0xFFFF) - 5;
                    vTextOutU(fp, s3, i32(p2));
                    p0 = (g0 & 0xFFFF) - 1;
                    if (p3 == 0) {
                        p0 = 38;
                    }
                    g0 = p0 & 0xFFFF;
                    if ((short) fp <= u16(g_18d20)) {
                        vSetClipWindow(0, 0, 128, 160);
                        sp += 40;
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * fun_1123c @ 0x1123c
     * called from: fun_11690
     */
    void fun_1123c(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, p1 = 0, r0 = 0;
        B_11298: {
            sp -= 32;
            fp = p0;
            icall(i32(g_18d38), p0);
            fun_104f4();
            if (fp <= 5) {
                p0 = g_18d2c;
                if (fp >= u16(p0)) {
                    put16(g_18d2a, fp - u16(p0) + 1);
                    break B_11298;
                }
            }
            put16(g_18d2a, 0);
        }
        s1 = g_18d2a;
        s0 = u16(s1);
        p1 = s0 & 0xFFFF;
        r0 = p1;
        s2 = g_18d2c;
        while (true) {
            if (p1 >= r0 + u16(s2)) {
                s1 = 0;
                s0 = 0;
                put8(fp * 12 + g_18d40, 0);
                fun_1100c(0);
                vFlipScreen(0);
                s2 = vGetTickCount();
                s3 = s2;
                s5 = 1;
                s4 = 500;
                while (true) {
                    B_113dc: {
                        B_11654: {
                            p1 = vGetButtonData();
                            if (p1 != 0) {
                                if ((s5 & 0xFF) != 0) {
                                    if (geu(s3 + s4, vGetTickCount())) {
                                        break B_113dc;
                                    }
                                    s4 = 70;
                                } else {
                                    s4 = 500;
                                }
                                B_113d0: {
                                    s5 = 1;
                                    s3 = vGetTickCount();
                                    if ((p1 & 4) == 0) {
                                        if ((p1 & 8) == 0) {
                                            if ((p1 & 0x10) == 0) {
                                                if ((p1 & 0x20) == 0) {
                                                    break B_11654;
                                                }
                                            } else {
                                                r0 = s1 & 0xFFFF;
                                                if (leu(r0, 35)) {
                                                    p0 = s0 & 0xFFFF;
                                                    if (gtu(p0, 2)) {
                                                        break B_113dc;
                                                    }
                                                    p1 = fp * 12;
                                                    put8(p0 + p1 + g_18d40, u8(i32((r0 << 2) + g_183e0)));
                                                    s0 = (s0 & 0xFFFF) + 1;
                                                    put8((s0 & 0xFFFF) + p1 + g_18d40, 0);
                                                    fun_10574(fp);
                                                    vSetForeColor(0x800003FF);
                                                    fun_105fc(fp);
                                                    break B_113d0;
                                                }
                                                if (r0 == 36) {
                                                    if ((s0 & 0xFFFF) != 0) {
                                                        s0 = (s0 & 0xFFFF) - 1;
                                                        put8((s0 & 0xFFFF) + fp * 12 + g_18d40, 0);
                                                    }
                                                    fun_10574(fp);
                                                    vSetForeColor(0x80007FFF);
                                                    fun_105fc(fp);
                                                    break B_113d0;
                                                }
                                                if (r0 == 37) {
                                                    r0 = s0 & 0xFFFF;
                                                    if (gtu(r0, 2)) {
                                                        break B_113dc;
                                                    }
                                                    p0 = fp * 12;
                                                    put8(r0 + p0 + g_18d40, 95);
                                                    s0 = (s0 & 0xFFFF) + 1;
                                                    put8((s0 & 0xFFFF) + p0 + g_18d40, 0);
                                                    fun_10574(fp);
                                                    vSetForeColor(0x800003FF);
                                                    fun_105fc(fp);
                                                    break B_113d0;
                                                }
                                                if (r0 != 38) {
                                                    break B_113dc;
                                                }
                                            }
                                            s0 = 0;
                                            while (true) {
                                                p0 = (s0 & 0xFFFF) + fp * 12 + g_18d40;
                                                if (u8(p0) == 95) {
                                                    put8(p0, 32);
                                                }
                                                s0 = (s0 & 0xFFFF) + 1;
                                                if ((s0 & 0xFFFF) > 2) {
                                                    sp += 32;
                                                    return;
                                                }
                                            }
                                        }
                                        p0 = (s1 & 0xFFFF) + 1;
                                        p0 -= p0 / 39 * 39;
                                    } else {
                                        p0 = (s1 & 0xFFFF) - 1;
                                        if ((s1 & 0xFFFF) == 0) {
                                            p0 = 38;
                                        }
                                    }
                                    s1 = p0 & 0xFFFF;
                                    fun_1100c(p0);
                                }
                                vFlipScreen(0);
                                break B_113dc;
                            }
                        }
                        s4 = 500;
                        s5 = 0;
                    }
                    if (ltu(s2 + 50, vGetTickCount())) {
                        s2 = vGetTickCount();
                        vSetForeColor(i32((s16(g_18d34) << 2) + g_18330));
                        fun_101a0();
                        fun_10f68(s1);
                        vFlipScreen(0);
                    }
                }
            }
            if (p1 == fp) {
                vSetForeColor(0x800003FF);
            } else {
                vSetForeColor(0x80007FFF);
            }
            fun_105fc(s0 & 0xFFFF);
            s0 = (s0 & 0xFFFF) + 1;
            p1 = s0 & 0xFFFF;
            r0 = u16(s1);
        }
    }
    
    /**
     * fun_11690 @ 0x11690
     * called from: fun_128a0
     */
    int fun_11690(int p0, int p1) {
        int fp = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, r0 = 0;
        sp -= 8;
        g2 = p0;
        g3 = p1;
        vSetClipWindow(0, 0, 128, 160);
        fp = 0;
        p2 = g_18d40;
        g0 = p2;
        p3 = g_18d4c;
        g1 = g_18d48;
        p1 = g_18d44;
        while (true) {
            p0 = fp & 0xFF;
            if (ltu(i32(p1), g2)) {
                if ((fp & 0xFF) <= 4) {
                    memcpy(p3, p2, (5 - p0) * 12);
                }
                memset(g0, 0, 12);
                put32(p1, g2);
                put8(g1, g3);
                fun_1123c(p0);
                r0 = (byte) fp;
                break;
            }
            g0 += 12;
            p2 += 12;
            p3 += 12;
            g1 += 12;
            p1 += 12;
            fp = (fp & 0xFF) + 1;
            if ((fp & 0xFF) > 5) {
                r0 = -1;
                break;
            }
        }
        sp += 8;
        return r0;
    }
    
    /**
     * fun_11758 @ 0x11758
     * called from: fun_12298, fun_128a0
     */
    void fun_11758() {
        int p0 = 0;
        sp -= 4;
        put8(g_186fc, 0);
        p0 = vStreamOpen(g_18c54, 3584) /* "resume" */;
        if (p0 != -1) {
            vStreamWrite(p0, g_186fc, 4);
            vStreamClose(p0);
        }
        sp += 4;
    }
    
    /**
     * fun_117a4 @ 0x117a4
     * called from: fun_11eac, fun_17b94
     */
    void fun_117a4(int p0) {
        int r0 = 0, r1 = 0;
        sp -= 4;
        p0 *= u8(g_1911c);
        r1 = u16(g_186f6);
        r0 = ((r1 - p0) & 0xFFFF) - 3;
        put16(g_19104, r0);
        if ((short) r0 < 0) {
            put16(g_19104, 0);
            put16(g_22e04, r1);
        } else {
            put16(g_22e04, p0);
        }
        p0 = u16(g_186f4);
        if ((p0 & 0xFFFF) <= 127) {
            put16(g_18d98, 0);
            put16(g_22e08, p0);
        } else {
            put16(g_18d98, 10);
            put16(g_22e08, 108);
        }
        sp += 4;
    }
    
    /**
     * fun_11844 @ 0x11844
     * called from: fun_12298, fun_12514, fun_128a0
     */
    void fun_11844(int p0, int p1) {
        int p2 = 0, g0 = 0, r0 = 0;
        sp -= 4;
        p2 = p1;
        g0 = vSetTransferMode(1);
        vSetForeColor(255);
        r0 = vTextExtentU(p2) & 0xFF;
        vTextOutU((u16(g_186f4) - r0) >> 1, p0 & 0xFF, p2);
        vSetTransferMode(g0);
        sp += 4;
    }
    
    /**
     * fun_118b4 @ 0x118b4  (address taken)
     */
    void fun_118b4() {
        sp -= 4;
        fun_11afc();
        sp += 4;
    }
    
    /**
     * fun_118c4 @ 0x118c4
     * called from: fun_12298, fun_12514
     */
    int fun_118c4() {
        int r0 = 0;
        sp -= 4;
        while (true) {
            r0 = vGetButtonData();
            if (r0 == 0) {
                sp += 4;
                return r0;
            }
        }
    }
    
    /**
     * fun_118d8 @ 0x118d8
     * called from: fun_12298
     */
    void fun_118d8() {
        sp -= 4;
        while (true) {
            if (vGetButtonData() != 0) {
                sp += 4;
                return;
            }
        }
    }
    
    /**
     * fun_118ec @ 0x118ec  (address taken)
     * called from: main
     */
    int fun_118ec() {
        int p1 = 0, p2 = 0, p3 = 0, r0 = 0;
        sp -= 4;
        p3 = vStreamOpen(g_18c54, 3840) /* "resume" */;
        if (p3 != -1) {
            vStreamWrite(p3, g_186fc, 4);
        }
        r0 = u8(g_186fc);
        if ((r0 & 0xFF) != 0) {
            p1 = g_18700;
            p2 = g_18c04 - p1;
            put32(g_18bfc, fun_118ec);
            put32(g_18c00, i32(g_184bc));
            if (p3 == -1) {
                r0 = 0;
                put8(g_186fc, r0);
            } else {
                r0 = vStreamWrite(p3, p1, p2);
            }
        }
        if (p3 != -1) {
            vStreamClose(p3);
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_1199c @ 0x1199c
     * called from: fun_121dc
     */
    void fun_1199c() {
        int p0 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, g5 = 0, g6 = 0, r0 = 0;
        B_11ad8: {
            sp -= 4;
            g0 = g_18700;
            p3 = g_18c04 - g0;
            p0 = vStreamOpen(g_18c54, 1280) /* "resume" */;
            if (p0 != -1) {
                vStreamSeek(p0, 4, 0);
                vStreamRead(p0, g0, p3);
                vStreamClose(p0);
                g6 = fun_118ec - i32(g_18bfc);
                g5 = i32(g_184bc) - i32(g_18c00);
                g0 = 0;
                g1 = g_18724;
                g4 = g_18730;
                g3 = g_1872e;
                g2 = g_18738;
                while (true) {
                    r0 = i32(g1);
                    if (r0 == 0) {
                        vSpriteSet(g0, 0, 0, 0);
                    } else {
                        put32(g1, r0 + g5);
                        r0 = i32(g2) + g6;
                        put32(g2, r0);
                        if (g0 <= 47) {
                            if (r0 != fun_14834) {
                                vSpriteSet(g0, i32(g1), u16(g3) - u16(g_1870e), u16(g4) - u16(g_18710));
                            }
                        }
                    }
                    g1 += 24;
                    g4 += 24;
                    g3 += 24;
                    g2 += 24;
                    g0++;
                    if (g0 > 50) {
                        break B_11ad8;
                    }
                }
            }
        }
        sp += 4;
    }
    
    /**
     * fun_11afc @ 0x11afc
     * called from: fun_118b4
     */
    void fun_11afc() {
        sp -= 4;
        vTerminateVMGP();
        throw terminated();
    }
    
    /**
     * fun_11b08 @ 0x11b08
     * called from: fun_11db4, fun_12dd4, fun_130e4
     */
    int fun_11b08(int p0, int p1, int p2) {
        int p3 = 0, g0 = 0, g1 = 0, g2 = 0;
        sp -= 4;
        g1 = p0 & 0xFF;
        p3 = p1 & 0xFFFF;
        g2 = p2;
        p0 = 0;
        g0 = vStreamOpen(p0, ((g1 & 0xFF) << 16) | 0x107);
        if (g0 == -1) {
            vMsgBoxU(8, g_23a10);
            vTerminateVMGP();
            throw terminated();
        }
        p2 = (short) p3;
        if (p2 == 0) {
            p3 = vDecompress(p0, g2, g0, 1024) & 0xFFFF;
            if ((short) p3 == -1) {
                vMsgBoxU(8, g_23a10);
                vTerminateVMGP();
                throw terminated();
            }
        } else {
            if ((g1 & 0xFF) == 0) {
                vTerminateVMGP();
                throw terminated();
            }
            vStreamRead(g0, g2, p2);
        }
        vStreamClose(g0);
        sp += 4;
        return (short) p3;
    }
    
    /**
     * fun_11bd8 @ 0x11bd8
     * called from: fun_14b3c
     */
    void fun_11bd8() {
        sp -= 4;
        if (u8(g_18701) == 0) {
            vPlayResource(g_17fe7, 81, 2) /* "MThd" */;
            put8(g_18701, 12);
        }
        sp += 4;
    }
    
    /**
     * fun_11c18 @ 0x11c18
     * called from: fun_14a74
     */
    int fun_11c18() {
        int r0 = 0;
        sp -= 4;
        r0 = u8(g_18701);
        if ((r0 & 0xFF) == 0) {
            vPlayResource(g_18270, 192, 2) /* "MThd" */;
            r0 = 4;
            put8(g_18701, r0);
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_11c58 @ 0x11c58
     * called from: fun_16580, fun_16a10, fun_16ecc
     */
    void fun_11c58() {
        sp -= 4;
        if (u8(g_18da8) != 0) {
            vPlayResource(g_17f93, 84, 2) /* "MThd" */;
        }
        sp += 4;
    }
    
    /**
     * fun_11c84 @ 0x11c84
     * called from: fun_125ec, fun_157e0
     */
    void fun_11c84() {
        sp -= 4;
        if (u8(g_18701) == 0) {
            vPlayResource(g_17ea0, 163, 2) /* "MThd" */;
            put8(g_18701, 12);
        }
        sp += 4;
    }
    
    /**
     * fun_11cc4 @ 0x11cc4
     * called from: fun_14144
     */
    void fun_11cc4() {
        sp -= 4;
        if (u8(g_18701) == 0) {
            vPlayResource(g_17f43, 80, 2) /* "MThd" */;
            put8(g_18701, 5);
        }
        sp += 4;
    }
    
    /**
     * fun_11d04 @ 0x11d04
     * called from: fun_15370
     */
    void fun_11d04() {
        sp -= 4;
        if (u8(g_18701) == 0) {
            vPlayResource(g_18038, 132, 2) /* "MThd" */;
        }
        sp += 4;
    }
    
    /**
     * fun_11d38 @ 0x11d38
     * called from: fun_14834
     */
    void fun_11d38() {
        sp -= 4;
        vPlayResource(g_180bc, 288, 2) /* "MThd" */;
        put8(g_18701, 20);
        sp += 4;
    }
    
    /**
     * fun_11d64 @ 0x11d64
     * called from: fun_14144
     */
    void fun_11d64() {
        sp -= 4;
        if (u8(g_18701) == 0) {
            vPlayResource(g_181dc, 148, 2) /* "MThd" */;
            put8(g_18701, 6);
        }
        sp += 4;
    }
    
    /**
     * fun_11da4 @ 0x11da4
     * called from: fun_11eac, fun_12164, fun_121dc
     */
    void fun_11da4() {
        sp -= 4;
        fun_16f28();
        sp += 4;
    }
    
    /**
     * fun_11db4 @ 0x11db4
     * called from: fun_11eac, fun_128a0
     */
    void fun_11db4() {
        int p0 = 0, p1 = 0, p2 = 0;
        sp -= 4;
        p2 = g_1bef7 & 0xFFFFFFFC;
        put32(g_1910c, p2);
        put32(g_18dbc, p2 + 768);
        fun_11b08(11, 0, p2);
        fun_11b08(12, 0, i32(g_18dbc));
        fun_1276c(i32(g_18dbc));
        p0 = g_18dd0;
        memcpy(p0, i32(g_22e00), 24);
        put32(g_18de4, i32(g_1910c));
        put8(g_19100, 0);
        vMapInit(p0);
        p1 = g_18dd8;
        put16(p1, 0);
        vMapInit(p0);
        if (u16(g_186f4) > 128) {
            put16(p1, 128);
            vMapInit(p0);
        }
        sp += 4;
    }
    
    /**
     * fun_11e9c @ 0x11e9c  (address taken)
     */
    void fun_11e9c() {
        sp -= 4;
        vUpdateMap();
        sp += 4;
    }
    
    /**
     * fun_11eac @ 0x11eac  (address taken)
     */
    int fun_11eac() {
        int s0 = 0, s1 = 0, p0 = 0, p2 = 0, r0 = 0;
        sp -= 60;
        vClearScreen(0x80001800);
        if (u8(g_19100) != 0) {
            fun_11da4();
            fun_11db4();
        }
        put32(sp + 20, g_23690);
        put32(sp + 24, g_23650);
        put32(sp + 28, g_236d0);
        put32(sp + 32, g_23b10);
        s0 = 5 - ((u8(g_18ce5) - 1) >>> 31);
        put32(sp + 36, g_23ad0);
        s1 = sp + 20;
        put32(s1 + (s0 << 2), g_23710);
        fun_10010(u16(i32(sp + 20)));
        p0 = s0;
        if (u8(g_186fc) != 0) {
            p0 = s0 + 1;
        }
        fun_117a4(p0);
        p0 = s0 & 0xFFFF;
        if (u8(g_186fc) == 0) {
            p2 = sp + 24;
        } else {
            p0 = (p0 & 0xFFFF) + 1;
            p2 = s1;
        }
        put16(sp, u16(g_19104));
        put16(sp + 4, u16(g_22e08));
        put16(sp + 8, u16(g_22e04));
        put16(sp + 12, 0);
        put32(sp + 16, fun_11e9c);
        p0 = (short) fun_17374(p0, 0, p2, u16(g_18d98));
        if (p0 == -1) {
            vTerminateVMGP();
            throw terminated();
        }
        if (u8(g_186fc) != 0) {
            p0--;
        }
        B_12050: {
            r0 = p0 + 1;
            if (leu(r0, 5)) {
                r0 <<= 2;
                switch (r0 >>> 2) {
                    case 0:
                        put32(g_19128, fun_121dc);
                        break B_12050;
                    case 1:
                        r0 = fun_12164;
                        put32(g_19128, r0);
                        break B_12050;
                    case 2:
                        r0 = fun_10a54(-1);
                        break B_12050;
                    case 3:
                        put32(sp, g_23b10);
                        r0 = vMsgBoxU(161, g_23210);
                        break B_12050;
                    case 4:
                        fun_10010(-3792);
                        s1 = g_18c5c;
                        put8(g_1911c, vTextExtentU(s1) >> 16);
                        s0 = fun_11e9c;
                        fun_17b94(s0);
                        fun_10010(u16(g_23ad0));
                        put8(g_1911c, vTextExtentU(s1) >> 16);
                        put32(sp, s0);
                        r0 = fun_1022c(0, 0, u16(g_186f4), u16(g_186f6));
                        break B_12050;
                    case 5:
                        vTerminateVMGP();
                        throw terminated();
                    default:
                        throw unreachable(0x1203c);
                }
            }
        }
        sp += 60;
        return r0;
    }
    
    /**
     * fun_12164 @ 0x12164  (address taken)
     */
    int fun_12164() {
        int r0 = 0;
        sp -= 4;
        fun_11da4();
        put8(g_18703, 3);
        put8(g_18702, 39);
        put16(g_18706, 0);
        fun_12698();
        vSpriteClear();
        put8(g_186fc, 1);
        put32(g_19128, fun_13220);
        put8(g_18c05, 1);
        r0 = 0;
        put8(g_186fe, r0);
        sp += 4;
        return r0;
    }
    
    /**
     * fun_121dc @ 0x121dc  (address taken)
     */
    int fun_121dc() {
        int r0 = 0;
        sp -= 4;
        fun_11da4();
        fun_12698();
        vSpriteClear();
        fun_13220();
        fun_1199c();
        r0 = 1;
        put8(g_18c05, r0);
        sp += 4;
        return r0;
    }
    
    /**
     * fun_12298 @ 0x12298  (address taken)
     */
    int fun_12298() {
        int r0 = 0;
        sp -= 4;
        vClearScreen(0);
        fun_11844((u16(g_186f6) >>> 1) - u8(g_1911c), (u8(g_186fe) << 6) + g_23750);
        put8(g_186fe, u8(g_186fe) + 1);
        fun_12698();
        vSpriteClear();
        put8(g_18702, 39);
        vFlipScreen(0);
        if (u8(g_186fe) <= 3) {
            fun_13220();
        } else {
            put32(g_19128, fun_128a0);
            fun_11758();
        }
        fun_118d8();
        r0 = fun_118c4();
        sp += 4;
        return r0;
    }
    
    /**
     * fun_1236c @ 0x1236c
     * called from: fun_123b8
     */
    int fun_1236c(int p0) {
        int p1 = 0, p2 = 0, r0 = 0, r1 = 0;
        sp -= 4;
        r1 = 0;
        p1 = 0;
        p2 = 15;
        while (true) {
            p1 <<= 1;
            r1 = (r1 << 2) | (p0 >>> 30);
            p0 <<= 2;
            r0 = (p1 << 1) + 1;
            if (r1 >= r0) {
                p1++;
                r1 -= r0;
            }
            p2--;
            if (p2 < 0) {
                sp += 4;
                return p1;
            }
        }
    }
    
    /**
     * fun_123b8 @ 0x123b8
     * called from: fun_12dd4
     */
    void fun_123b8() {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, p0 = 0, p1 = 0, r0 = 0;
        sp -= 24;
        s2 = 0;
        s0 = -256;
        s1 = 32;
        s3 = 32;
        fp = 0;
        while (true) {
            r0 = fun_1236c(s0 * s0 + s2 * s2);
            if ((fp & 0xFF) == 8) {
                s1 = -s1;
            }
            if ((fp & 0xFF) == 16) {
                s3 = -s3;
            }
            if ((fp & 0xFF) == 24) {
                s1 = -s1;
            }
            p1 = (fp & 0xFF) << 2;
            p0 = div(0x10000, r0);
            put16(p1 + g_22d70, (s2 * p0) >> 7);
            put16(p1 + g_22d72, (s0 * p0) >> 7);
            s2 += s1;
            s0 += s3;
            fp = (fp & 0xFF) + 1;
            if ((fp & 0xFF) > 31) {
                sp += 24;
                return;
            }
        }
    }
    
    /**
     * fun_12460 @ 0x12460
     * called from: fun_158c0, fun_16bbc
     */
    int fun_12460(int p0, int p1, int p2, int p3) {
        B_12484: {
            sp -= 4;
            p0 = (p2 & 0xFFFF) - (p0 & 0xFFFF);
            p3 = (p3 & 0xFFFF) - (p1 & 0xFFFF);
            p1 = 0;
            if ((short) p0 < 0) {
                p0 = -p0;
                p1 = 128;
                if ((short) p3 >= 0) {
                    break B_12484;
                }
                p1 = -64;
            } else if ((short) p3 >= 0) {
                p1 = 64;
                break B_12484;
            }
            p3 = -p3;
        }
        p2 = (short) p0;
        if (p2 == 0) {
            if ((p1 & 0x40) != 0) {
                p1 = (p1 & 0xFF) + 64;
            }
        } else if ((p1 & 0x40) == 0) {
            p1 = (p1 & 0xFF) + (div(p2 << 6, p2 + (short) p3) & 0xFF);
        } else {
            p1 = ((p1 - div(p2 << 6, p2 + (short) p3)) & 0xFF) + 64;
        }
        sp += 4;
        return ((p1 & 0xFF) >>> 3) & 0xFF;
    }
    
    /**
     * fun_12514 @ 0x12514  (address taken)
     */
    int fun_12514(int p0) {
        int s0 = 0, r0 = 0;
        sp -= 12;
        if (u8(g_18d90) != 0) {
            put8(g_18617, 1);
            icall(i32(g_19124), p0);
            put8(g_18d90, 0);
            icall(i32(g_19124), p0);
        }
        s0 = g_18617;
        put8(s0, 1);
        if ((u8(g_186ff) & 4) != 0) {
            put8(s0, 9);
        }
        fun_11844(u16(g_186f6) >>> 1, g_23a90);
        r0 = i32(g_19118) & 0x10;
        if (r0 != 0) {
            vFlipScreen(0);
            fun_118c4();
            put32(g_19128, i32(g_19124));
            r0 = 1;
            put8(s0, r0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_125ec @ 0x125ec  (address taken)
     */
    int fun_125ec() {
        int p0 = 0, p1 = 0, r0 = 0;
        sp -= 4;
        r0 = g_18703;
        p0 = u8(r0) + 255;
        put8(r0, p0);
        if ((p0 & 0xFF) == 0) {
            put32(g_19128, fun_128a0);
        } else {
            put32(g_19128, fun_12514);
        }
        fun_11c84();
        put8(g_18d90, 1);
        p0 = u8(g_18718);
        if ((p0 & 0xFF) != 0) {
            p1 = 0;
            put32((p0 & 0xFF) * 24 + g_18724, p1);
            vSpriteSet(p0, p1, 0, 0);
            put8(g_18718, 0);
        }
        r0 = fun_12988();
        sp += 4;
        return r0;
    }
    
    /**
     * fun_12698 @ 0x12698
     * called from: fun_12164, fun_121dc, fun_12298
     */
    void fun_12698() {
        int g0 = 0, g1 = 0;
        sp -= 4;
        g0 = 0;
        g1 = g_18724;
        while (true) {
            put32(g1, 0);
            g1 += 24;
            vSpriteSet(g0, 0, 0, 0);
            g0 = (g0 & 0xFF) + 1;
            if ((g0 & 0xFF) > 47) {
                sp += 4;
                return;
            }
        }
    }
    
    /**
     * fun_126dc @ 0x126dc
     * called from: fun_133c0
     */
    int fun_126dc() {
        int r0 = 0, r1 = 0;
        sp -= 4;
        r1 = 1;
        while (true) {
            r0 = r1 & 0xFF;
            if (i32(r0 * 24 + g_18724) == 0) {
                break;
            }
            r1 = (r1 & 0xFF) + 1;
            if ((r1 & 0xFF) > 31) {
                r0 = 0;
                break;
            }
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_1270c @ 0x1270c
     * called from: fun_133c0, fun_13bc4, fun_14f94, fun_15c78, fun_16bbc, fun_16cd8, fun_16d78, fun_16e18
     */
    int fun_1270c() {
        int r0 = 0, r1 = 0;
        sp -= 4;
        r1 = 34;
        while (true) {
            r0 = r1 & 0xFF;
            if (i32(r0 * 24 + g_18724) == 0) {
                break;
            }
            r1 = (r1 & 0xFF) + 1;
            if ((r1 & 0xFF) > 47) {
                r0 = 0;
                break;
            }
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_1273c @ 0x1273c
     * called from: fun_14014, fun_140b4
     */
    int fun_1273c() {
        int r0 = 0, r1 = 0;
        sp -= 4;
        r1 = 47;
        while (true) {
            r0 = r1 & 0xFF;
            if (i32(r0 * 24 + g_18724) == 0) {
                break;
            }
            r1 = (r1 & 0xFF) + 255;
            if ((r1 & 0xFF) <= 33) {
                r0 = 0;
                break;
            }
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_1276c @ 0x1276c
     * called from: fun_11db4, fun_130e4, fun_13220
     */
    void fun_1276c(int p0) {
        int p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, r0 = 0, r1 = 0;
        B_127b8: {
            sp -= 4;
            g0 = p0;
            put32(g_18e00, g0);
            p3 = 8;
            put32(g_22e00, g0 + p3);
            r0 = u8(g0 + 2);
            put8(g_22df0, r0);
            p2 = r0 & 0xFF;
            if (p2 != 0) {
                r1 = p2;
                while (true) {
                    p3 += 24;
                    r1--;
                    if (r1 == 0) {
                        break B_127b8;
                    }
                }
            }
        }
        p0 = i32(g_18e00);
        if (s16(p0 + 6) == 0) {
            put32(g_22df4, 0);
        } else {
            put32(g_22df4, g0 + p3);
            p3 += (s16(p0 + 6) << 3);
        }
        B_12888: {
            g1 = u8(i32(g_22e00) + 5);
            r1 = 0;
            if (r1 < u8(g_22df0)) {
                while (true) {
                    p2 = r1 * 24;
                    p1 = p2 + i32(g_22e00);
                    put8(p1 + 5, s8(p1 + 5) - g1);
                    p2 += i32(g_22e00);
                    p0 = u8(p2 + 3) * u8(p2 + 2);
                    if (u8(p2) != 0) {
                        p0 <<= 1;
                    }
                    put32(p2 + 16, g0 + p3);
                    p3 += p0;
                    r1++;
                    if (r1 >= u8(g_22df0)) {
                        break B_12888;
                    }
                }
            }
        }
        sp += 4;
    }
    
    /**
     * fun_128a0 @ 0x128a0  (address taken)
     */
    int fun_128a0() {
        int p0 = 0, r0 = 0;
        sp -= 4;
        fun_11758();
        vClearScreen(0);
        vSetForeColor(255);
        fun_11844((u16(g_186f6) - u8(g_1911c)) >> 1, g_239d0);
        vFlipScreen(0);
        while (true) {
            if (vGetButtonData() != 16) {
                while (true) {
                    if (vGetButtonData() == 16) {
                        fun_11db4();
                        p0 = (byte) fun_11690(u16(g_18706), u8(g_186fe) + 1) & 0xFFFF;
                        fun_10a54(p0);
                        fun_10ea0();
                        put32(g_19128, fun_11eac);
                        r0 = 0;
                        put8(g_186fe, r0);
                        sp += 4;
                        return r0;
                    }
                    sleep();
                }
            }
            sleep();
        }
    }
    
    /**
     * fun_12988 @ 0x12988
     * called from: fun_125ec, fun_13220
     */
    int fun_12988() {
        int r0 = 0;
        sp -= 4;
        put8(g_18702, 39);
        put16(g_18708, u16(g_18d94));
        put16(g_1870a, u16(g_18d9c));
        r0 = 0;
        put8(g_18713, r0);
        put8(g_18712, r0);
        put16(g_1870c, u16(g_18d9c));
        fun_13dac();
        fun_13048();
        put32(g_18a38, fun_14144);
        put32(g_18a24, i32(g_184bc));
        r0 = 0;
        put16(g_18a34, r0);
        put8(g_18a28, r0);
        put8(g_18a29, 30);
        put16(g_18bf0, 12);
        put16(g_18bf2, 14);
        put16(g_18bf8, 50);
        r0 = 4;
        put16(g_18bfa, r0);
        sp += 4;
        return r0;
    }
    
    /**
     * fun_12a60 @ 0x12a60
     * called from: fun_12d50
     */
    int fun_12a60() {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, p3 = 0, g0 = 0, r0 = 0;
        sp -= 24;
        icall(i32(g_18a38), 32);
        s3 = 0;
        fp = s3 & 0xFF;
        s2 = g_18738;
        s1 = g_18724;
        while (true) {
            r0 = i32(s1);
            s1 += 24;
            if (r0 != 0) {
                s0 = i32(s2);
                if (s0 == fun_14920) {
                    s3 = fp & 0xFF;
                } else if ((fp & 0xFF) != 32) {
                    icall(s0, fp);
                }
            }
            s2 += 24;
            fp = (fp & 0xFF) + 1;
            if ((fp & 0xFF) > 47) {
                fun_15790();
                vMapSetXY(u16(g_1870e), u16(g_18710));
                vSetClipWindow(u16(g_186f8), u16(g_186fa), u16(g_186f4), u16(g_186f6) - 17);
                vUpdateMap();
                vSetClipWindow(u16(g_186f8), u16(g_186fa), u16(g_186f4), u16(g_186f6));
                vUpdateSprite();
                put16(g_18df2, u16(g_186f6) - 24);
                vMapInit(g_18de8);
                p3 = u16(g_186f6);
                vSetClipWindow(u16(g_186f8), p3 - 24, u16(g_186f4), p3);
                vUpdateMap();
                vSetClipWindow(u16(g_186f8), u16(g_186fa), u16(g_186f4), u16(g_186f6));
                vMapInit(g_18dd0);
                if ((s3 & 0xFF) != 0) {
                    icall(i32((s3 & 0xFF) * 24 + g_18738), s3);
                }
                r0 = u8(g_18702);
                if ((r0 & 0xFF) != 0) {
                    r0 = ((byte) ((byte) r0 >> 3) << 2) + g_18538;
                    put32(g_18bd4, i32(r0));
                    vDrawObject(4, u16(g_186f6) - 20, i32(r0));
                }
                g0 = g_18db0;
                vutoa(s8(g_18703) - 1, g0, 2, 48);
                if (u16(g_186f4) > 127) {
                    vPrint(1, 44, u16(g_186f6) - 10, g0);
                } else {
                    vPrint(1, 33, u16(g_186f6) - 10, g0);
                }
                g0 = g_18db0;
                vutoa(u16(g_18706), g0, 5, 48);
                r0 = u16(g_186f4);
                if (gtu(r0, 127)) {
                    vPrint(1, 80, u16(g_186f6) - 14, g0);
                } else {
                    vPrint(1, r0 - 41, u16(g_186f6) - 10, g0);
                }
                sp += 24;
                return r0;
            }
        }
    }
    
    /**
     * fun_12d50 @ 0x12d50  (address taken)
     */
    int fun_12d50() {
        int r0 = 0;
        sp -= 4;
        vClearScreen(u16((u8(g_18dc4) << 1) + g_186c4) | 0x80000000);
        if ((u8(g_186ff) & 0x1F) == 31) {
            fun_13048();
        }
        fun_13bc4();
        r0 = fun_12a60();
        sp += 4;
        return r0;
    }
    
    /**
     * fun_12dac @ 0x12dac  (address taken)
     */
    void fun_12dac() {
        int p0 = 0;
        sp -= 4;
        p0 = 0;
        vSetForeColor(p0);
        vFillRect(p0, 0, 128, 160);
        sp += 4;
    }
    
    /**
     * fun_12dd4 @ 0x12dd4
     * called from: main
     */
    int fun_12dd4() {
        int s0 = 0, p0 = 0, p2 = 0, r0 = 0;
        sp -= 36;
        vSetBackColor(0x800001EF);
        vSetForeColor(255);
        fun_11b08(16, 0, g_18e10);
        put8(g_18617, 1);
        vSetActiveFont(g_1860c);
        r0 = 0;
        while (true) {
            p2 = (r0 & 0xFF) + 1;
            vSetPaletteEntry(p2, u16(((r0 & 0xFF) << 1) + g_18674));
            r0 = p2 & 0xFF;
            if ((r0 & 0xFF) > 39) {
                p0 = vStreamOpen(g_18c54, 1280) /* "resume" */;
                if (p0 != -1) {
                    vStreamRead(p0, g_186fc, 4);
                    vStreamClose(p0);
                }
                put16(sp + 4, 8);
                vGetCaps(0, sp + 4);
                put16(g_186f4, u16(sp + 8));
                r0 = u16(sp + 10);
                put16(g_186f6, r0);
                if ((r0 & 0xFFFF) > 216) {
                    put16(g_186f6, 216);
                }
                fun_10010(-3792);
                s0 = g_18c5c;
                put8(g_1911c, vTextExtentU(s0) >> 16);
                fun_17d90(fun_12dac);
                fun_10010(u16(g_23ad0));
                put8(g_1911c, vTextExtentU(s0) >> 16);
                put32(sp, fun_11e9c);
                fun_1022c(0, 0, u16(g_186f4), u16(g_186f6));
                fun_10f00();
                put32(g_19128, fun_11eac);
                put8(g_19100, 2);
                put16(sp + 12, 12);
                vGetCaps(4, sp + 12);
                put8(g_19110, 0);
                if (u16(sp + 16) == 3) {
                    if ((i32(sp + 20) & 4) != 0) {
                        put8(g_19110, 1);
                    }
                    put8(g_18da8, 1);
                }
                vSpriteInit(48);
                fun_11b08(17, 0, g_19130);
                fun_123b8();
                sp += 36;
                return 0;
            }
        }
    }
    
    /**
     * fun_13048 @ 0x13048
     * called from: fun_12988, fun_12d50
     */
    void fun_13048() {
        int p0 = 0, p1 = 0, r0 = 0;
        sp -= 12;
        p1 = g_18bbc;
        vGetTimeDate(sp);
        put8(g_18dc4, u8(sp + 5));
        r0 = 20;
        put16(g_18bc8, r0);
        put16(g_18bca, r0);
        p0 = u8(sp + 5);
        if (((p0 + 250) & 0xFF) > 12) {
            r0 = i32(g_18528);
        } else {
            r0 = (p0 & 0xFF) + 244;
            if ((byte) r0 < 0) {
                r0 = -r0;
            }
            put16(g_18bc8, (byte) r0 << 3);
            r0 = i32(g_18524);
        }
        put32(p1, r0);
        sp += 12;
    }
    
    /**
     * fun_130e4 @ 0x130e4
     * called from: fun_13220
     */
    int fun_130e4() {
        int p2 = 0, r0 = 0;
        sp -= 4;
        r0 = ((short) fun_11b08(u8(g_186fe), 0, g_1bef4) + g_1bef7) & 0xFFFFFFFC;
        put32(g_1910c, r0);
        r0 += 0x13C0;
        put32(g_18dbc, r0);
        fun_11b08(u8(g_186fe) + 4, 0, r0);
        fun_11b08(10, 0, i32(g_1910c));
        r0 = i32(g_18dbc);
        put32(g_18dc0, r0 + 0x2960);
        p2 = (r0 + 0x2EE3) & 0xFFFFFFFC;
        put32(g_19108, p2);
        if (u16(g_186f4) > 127) {
            fun_11b08(13, 0, p2);
        } else {
            fun_11b08(14, 0, p2);
        }
        fun_11b08(15, 0, i32(g_18dc0));
        fun_1276c(i32(g_19108));
        memcpy(g_18de8, i32(g_22e00), 24);
        put32(g_18dfc, i32(g_18dc0));
        sp += 4;
        return 0;
    }
    
    /**
     * fun_13220 @ 0x13220  (address taken)
     * called from: fun_121dc, fun_12298
     */
    int fun_13220() {
        int s0 = 0, p0 = 0, r0 = 0;
        sp -= 12;
        put8(g_18dc4, 0);
        r0 = fun_130e4();
        if ((r0 & 0xFF) == 0) {
            fun_1276c(i32(g_18dbc));
            r0 = u8(g_186fe);
            if ((r0 & 0xFF) == 0) {
                put8(g_19114, 31);
            } else if ((r0 & 0xFF) == 1) {
                put8(g_19114, 15);
            } else if ((r0 & 0xFF) == 2) {
                put8(g_19114, 7);
            } else if ((r0 & 0xFF) == 3) {
                put8(g_19114, 3);
            }
            s0 = g_18dd0;
            p0 = i32(g_22e00);
            memcpy(s0, p0, 24);
            put32(g_18de4, i32(g_1910c));
            put16(g_19120, (u8(p0 + 2) << 3) - u16(g_186f4));
            put16(g_18da4, (u8(p0 + 3) << 3) - u16(g_186f6) + 16);
            p0 = vGetTickCount();
            vSetRandom(p0);
            fun_133c0(0);
            fun_12988();
            put32(g_19124, i32(u8(g_186fe) * 12 + g_18648));
            put32(g_19128, fun_12514);
            put8(g_18d90, 1);
            r0 = vMapInit(s0);
            vSetClipWindow(u16(g_186f8), u16(g_186fa), u16(g_186f4), u16(g_186f6));
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_133c0 @ 0x133c0
     * called from: fun_13220
     */
    void fun_133c0(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, r0 = 0;
        B_134c0: {
            sp -= 20;
            p1 = 0;
            put16(g_1871e, p1);
            put16(g_18720, p1);
            s2 = p0 & 0xFF;
            p0 = s2 * 24 + i32(g_22e00);
            s0 = u8(p0 + 5);
            p1 = s0 & 0xFF;
            if (p1 < u8(p0 + 6) + p1) {
                while (true) {
                    B_13488: {
                        p0 = i32(g_22df4);
                        r0 = i32((p1 << 3) + p0) & 0x1F;
                        if (leu(r0, 15)) {
                            B_13b08: {
                                switch (r0) {
                                    case 0:
                                        r0 = ((s0 & 0xFF) << 3) + p0;
                                        put16(g_18d94, u16(r0 + 4) << 3);
                                        put16(g_18d9c, (u16(r0 + 6) << 3) - 4);
                                        break B_13488;
                                    case 1:
                                    case 2:
                                        g1 = fun_126dc() & 0xFF;
                                        g0 = (g1 & 0xFF) * 24 + g_18724;
                                        if ((g1 & 0xFF) == 0) {
                                            break B_13488;
                                        }
                                        B_135bc: {
                                            put32(g0, i32(g_1852c));
                                            put32(g0 + 20, fun_15450);
                                            p2 = s0 & 0xFF;
                                            p0 = p2 << 3;
                                            p1 = p0 + i32(g_22df4);
                                            put16(g0 + 10, u16(p1 + 4) << 3);
                                            put16(g0 + 12, u16(p1 + 6) << 3);
                                            put8(g0 + 4, 0);
                                            r0 = i32(p0 + i32(g_22df4)) >>> 28;
                                            put8(g0 + 5, r0);
                                            fp = u8(p0 + i32(g_22df4) + 3) & 3;
                                            put8(g0 + 7, r0 << 3);
                                            if ((fp & 0xFF) == 0) {
                                                put16(g0 + 14, 0);
                                                p0 = 2;
                                            } else {
                                                if ((fp & 0xFF) == 1) {
                                                    put16(g0 + 14, 2);
                                                    put16(g0 + 16, 0);
                                                    break B_135bc;
                                                }
                                                if ((fp & 0xFF) == 2) {
                                                    put16(g0 + 14, 0);
                                                    put16(g0 + 16, -2);
                                                    break B_135bc;
                                                }
                                                put16(g0 + 14, -2);
                                                p0 = 0;
                                            }
                                            put16(g0 + 16, p0);
                                        }
                                        fp = g1 & 0xFF;
                                        s1 = p2 << 3;
                                        if ((i32(s1 + i32(g_22df4)) & 0xFFFF00) == 0) {
                                            put8(g0 + 4, 1);
                                            break B_13488;
                                        }
                                        g1 = fun_126dc() & 0xFF;
                                        if ((g1 & 0xFF) == 0) {
                                            break B_13488;
                                        }
                                        g0 = (g1 & 0xFF) * 24 + g_18724;
                                        put32(g0, i32(g_18530));
                                        put32(g0 + 20, fun_15370);
                                        p0 = s1 + i32(g_22df4);
                                        put16(g0 + 10, ((i32(p0) >>> 8) & 0xFF) << 3);
                                        put16(g0 + 12, (u16(p0 + 2) & 0xFF) << 3);
                                        put8(g0 + 4, 0);
                                        put8(g0 + 5, fp);
                                        break B_13488;
                                    case 3:
                                        g1 = fun_126dc() & 0xFF;
                                        if ((g1 & 0xFF) != 0) {
                                            g0 = (g1 & 0xFF) * 24 + g_18724;
                                            put32(g0, i32(g_18594));
                                            put32(g0 + 20, fun_14a74);
                                            r0 = ((s0 & 0xFF) << 3) + i32(g_22df4);
                                            p2 = u16(r0 + 4) << 3;
                                            put16(g0 + 10, p2);
                                            p3 = u16(r0 + 6) << 3;
                                            put16(g0 + 12, p3);
                                            put16(g0 + 8, 100);
                                            break B_13b08;
                                        }
                                        break B_13488;
                                    case 4:
                                        g1 = fun_126dc() & 0xFF;
                                        if ((g1 & 0xFF) != 0) {
                                            g0 = (g1 & 0xFF) * 24 + g_18724;
                                            put32(g0, i32(g_185a4));
                                            put32(g0 + 20, fun_14b3c);
                                            r0 = ((s0 & 0xFF) << 3) + i32(g_22df4);
                                            p2 = u16(r0 + 4) << 3;
                                            put16(g0 + 10, p2);
                                            p3 = u16(r0 + 6) << 3;
                                            put16(g0 + 12, p3);
                                            break B_13b08;
                                        }
                                        break B_13488;
                                    case 5:
                                        g1 = fun_1270c() & 0xFF;
                                        if ((g1 & 0xFF) != 0) {
                                            g0 = (g1 & 0xFF) * 24 + g_18724;
                                            put32(g0, i32(g_18584));
                                            put32(g0 + 20, fun_14c3c);
                                            r0 = ((s0 & 0xFF) << 3) + i32(g_22df4);
                                            p2 = u16(r0 + 4) << 3;
                                            put16(g0 + 10, p2);
                                            p3 = u16(r0 + 6) << 3;
                                            put16(g0 + 12, p3);
                                            p1 = 0;
                                            put8(g0 + 7, p1);
                                            put8(g0 + 4, p1);
                                            put16(g0 + 8, 12);
                                            break B_13b08;
                                        }
                                        break B_13488;
                                    case 6:
                                        r0 = ((s0 & 0xFF) << 3) + p0;
                                        put16(g_1871e, u16(r0 + 4) << 3);
                                        put16(g_18720, u16(r0 + 6) << 3);
                                        put16(g_18722, 200);
                                        put32(g_1864c, fun_16cd8);
                                        break B_13488;
                                    case 7:
                                        r0 = ((s0 & 0xFF) << 3) + p0;
                                        put16(g_1871e, u16(r0 + 4) << 3);
                                        put16(g_18720, u16(r0 + 6) << 3);
                                        put16(g_18722, 100);
                                        put32(g_1864c, fun_16bbc);
                                        break B_13488;
                                    case 8:
                                        r0 = ((s0 & 0xFF) << 3) + p0;
                                        put16(g_1871e, u16(r0 + 4) << 3);
                                        put16(g_18720, u16(r0 + 6) << 3);
                                        put16(g_18722, 300);
                                        put32(g_1864c, fun_16d78);
                                        break B_13488;
                                    case 9:
                                        r0 = ((s0 & 0xFF) << 3) + p0;
                                        put16(g_1871e, u16(r0 + 4) << 3);
                                        put16(g_18720, (u16(r0 + 6) << 3) + 14);
                                        put16(g_18722, 400);
                                        put32(g_1864c, fun_16e18);
                                        break B_13488;
                                    case 10:
                                        g1 = fun_1270c() & 0xFF;
                                        if ((g1 & 0xFF) != 0) {
                                            g0 = (g1 & 0xFF) * 24 + g_18724;
                                            put32(g0, i32(g_1854c));
                                            put32(g0 + 20, fun_14f94);
                                            r0 = ((s0 & 0xFF) << 3) + i32(g_22df4);
                                            p2 = ((u16(r0 + 4) - 1) & 0xFFFF) << 3;
                                            put16(g0 + 10, p2);
                                            p3 = ((u16(r0 + 6) - 1) & 0xFFFF) << 3;
                                            put16(g0 + 12, p3);
                                            r0 = 0;
                                            put8(g0 + 7, r0);
                                            put8(g0 + 4, r0);
                                            put8(g0 + 5, r0);
                                            put16(g0 + 8, 20);
                                            break B_13b08;
                                        }
                                        break B_13488;
                                    case 11:
                                    case 12:
                                    case 13:
                                    case 14:
                                        break B_13488;
                                    case 15:
                                        g1 = fun_126dc() & 0xFF;
                                        if ((g1 & 0xFF) != 0) {
                                            g0 = (g1 & 0xFF) * 24 + g_18724;
                                            put32(g0, i32(g_1859c));
                                            put32(g0 + 20, fun_14834);
                                            p0 = ((s0 & 0xFF) << 3) + i32(g_22df4);
                                            put16(g0 + 10, u16(p0 + 4) << 3);
                                            put16(g0 + 12, u16(p0 + 6) << 3);
                                            put8(g0 + 4, 0);
                                            break B_13488;
                                        }
                                        break B_13488;
                                    default:
                                        throw unreachable(0x13448);
                                }
                            }
                            vSpriteSet(g1, i32(g0), p2, p3);
                        }
                    }
                    s0 = (s0 & 0xFF) + 1;
                    p1 = s0 & 0xFF;
                    r0 = s2 * 24 + i32(g_22e00);
                    if (p1 >= u8(r0 + 6) + u8(r0 + 5)) {
                        break B_134c0;
                    }
                }
            }
        }
        sp += 20;
    }
    
    /**
     * fun_13bc4 @ 0x13bc4
     * called from: fun_12d50
     */
    void fun_13bc4() {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, p0 = 0, r0 = 0;
        B_13c00: {
            sp -= 24;
            p0 = u8(g_186ff);
            if ((p0 & 0x20) != 0) {
                put8(g_186ff, p0 & 0x1F);
                p0 = u8(g_19114);
                if ((u16(g_18da0) & p0) == p0) {
                    r0 = vGetRandom();
                    put16(g_18da0, r0);
                    p0 = r0 & 3;
                    if (p0 == 0) {
                        put16(g_18da0, vGetRandom());
                        fp = u16(g_1870e) + u16(g_186f4);
                        s3 = u16(g_1870a);
                        s0 = 0;
                        s1 = 0;
                        s2 = u8(g_18da0) & 3;
                        if (ltu(p0, s2)) {
                            while (true) {
                                r0 = fun_1270c();
                                if ((r0 & 0xFF) == 0) {
                                    break B_13c00;
                                }
                                p0 = (r0 & 0xFF) * 24 + g_18724;
                                put32(p0, i32(g_1857c));
                                put32(p0 + 20, fun_14d68);
                                put8(p0 + 7, 0);
                                put8(p0 + 4, 1);
                                put16(p0 + 10, fp);
                                put16(p0 + 12, s3 + s0);
                                put16(p0 + 8, 14);
                                if ((s1 & 1) == 0) {
                                    fp = (fp & 0xFFFF) + 8;
                                    s0 = (s0 & 0xFFFF) + 8;
                                }
                                s0 = -s0;
                                s1 = (s1 & 0xFF) + 1;
                                if (geu(s1 & 0xFF, s2)) {
                                    break B_13c00;
                                }
                            }
                        }
                    } else {
                        r0 = fun_1270c();
                        if ((r0 & 0xFF) != 0) {
                            p0 = (r0 & 0xFF) * 24 + g_18724;
                            put32(p0, i32(g_18574));
                            put32(p0 + 20, fun_14e50);
                            put8(p0 + 7, 8);
                            put16(p0 + 16, 1);
                            put16(p0 + 10, u16(g_1870e) - 16);
                            put16(p0 + 12, u16(g_1870a));
                            put16(p0 + 8, 16);
                        }
                    }
                }
            }
        }
        sp += 24;
    }
    
    /**
     * fun_13dac @ 0x13dac
     * called from: fun_12988, fun_14144, fun_15450
     */
    int fun_13dac() {
        int p0 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, r0 = 0;
        sp -= 12;
        g0 = u16(g_186f4);
        p1 = ((u16(g_18708) - ((g0 & 0xFFFF) >>> 1)) & 0xFFFF) + 12;
        put16(g_1870e, p1);
        r0 = ((u16(g_1870a) - (u16(g_186f6) >>> 1)) & 0xFFFF) + 30;
        put16(g_18710, r0);
        if ((short) p1 < 0) {
            put16(g_1870e, 0);
        }
        if ((short) r0 < 0) {
            put16(g_18710, 0);
        }
        p2 = u16(g_19120);
        p3 = (short) p2;
        if (s16(g_1870e) > p3) {
            put16(g_1870e, p2);
        }
        p1 = u16(g_18da4);
        p0 = s16(g_18710);
        if (p0 > (short) p1) {
            put16(g_18710, p1);
        }
        B_13ee4: {
            if (u8(g_18712) == 0) {
                if (s16(g_1870e) == p3) {
                    if (s16(g_1871e) != 0) {
                        r0 = 1;
                        put8(g_18713, r0);
                        put8(g_18712, r0);
                        icall(i32(g_1864c), p0);
                    }
                }
            } else {
                B_13f98: {
                    B_13eb4: {
                        if (u8(g_186fe) == 3) {
                            if ((g0 & 0xFFFF) > 227) {
                                break B_13f98;
                            }
                            if (s16(g_1870e) >= s16(g_19120) + u16(g_186f4) - 228) {
                                break B_13eb4;
                            }
                            r0 = ((p2 + g0) & 0xFFFF) + 0xFF1C;
                        } else {
                            if ((g0 & 0xFFFF) > 127) {
                                break B_13f98;
                            }
                            if (s16(g_1870e) >= s16(g_19120) + u16(g_186f4) - 128) {
                                break B_13eb4;
                            }
                            r0 = ((p2 + g0) & 0xFFFF) - 128;
                        }
                        put16(g_1870e, r0);
                    }
                    if (s16(g_18708) >= s16(g_1870e) - 6) {
                        break B_13ee4;
                    }
                    put16(g_18708, u16(g_1870e) - 6);
                    break B_13ee4;
                }
                put16(g_1870e, p2);
            }
        }
        g0 = u16(g_18708) - u16(g_1870e);
        r0 = u16(g_18710);
        g1 = u16(g_1870a) - (r0 & 0xFFFF);
        vSpriteSet(32, i32(g_18a24), g0, g1);
        put16(g_18bec, g0 + 4);
        put16(g_18bee, g1 + 5);
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14014 @ 0x14014
     * called from: fun_16580
     */
    void fun_14014(int p0, int p1) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        fp = p0 & 0xFFFF;
        s0 = p1 & 0xFFFF;
        p0 = fun_1273c() & 0xFF;
        if ((p0 & 0xFF) != 0) {
            r0 = (p0 & 0xFF) * 24 + g_18724;
            p1 = g_18508;
            put32(r0, i32(p1));
            put8(r0 + 7, 4);
            put32(r0 + 20, fun_16460);
            put16(r0 + 10, fp);
            put16(r0 + 12, s0);
            vSpriteSet(p0, i32(p1), fp - u16(g_1870e), s0 - u16(g_18710));
        }
        sp += 12;
    }
    
    /**
     * fun_140b4 @ 0x140b4
     * called from: fun_16224
     */
    int fun_140b4(int p0, int p1) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        fp = p0 & 0xFFFF;
        s0 = p1 & 0xFFFF;
        r0 = fun_1273c();
        p0 = r0 & 0xFF;
        if ((p0 & 0xFF) != 0) {
            r0 = (p0 & 0xFF) * 24 + g_18724;
            p1 = g_18520;
            put32(r0, i32(p1));
            put8(r0 + 7, 6);
            put32(r0 + 20, fun_164f0);
            put16(r0 + 10, fp);
            put16(r0 + 12, s0);
            vSpriteSet(p0, i32(p1), fp, s0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14144 @ 0x14144  (address taken)
     */
    int fun_14144() {
        int fp = 0, s0 = 0, p0 = 0, p1 = 0, p2 = 0, p3 = 0, r0 = 0;
        sp -= 12;
        fp = g_18a24;
        s0 = 0;
        put32(fp, i32((u8(g_18a2b) << 2) + g_184bc));
        p1 = u8(g_18a2b);
        put8(g_18a2b, p1 & 0xF8);
        put16(g_1870c, u16(g_1870a));
        p0 = i32(g_19118);
        if ((p0 & 8) != 0) {
            s0 = 3;
            if ((p1 & 8) != 0) {
                s0 = 5;
            }
            put8(g_18a2b, (u8(g_186ff) & 3) + 1);
            p2 = (s16(g_1870a) + 8) >>> 3;
            p0 = ((s16(g_18708) + (short) s0 + 12) >>> 3) & 0xFF;
            p3 = vMapGetAttribute(p0, p2) & 0xFF;
            if (((p3 | vMapGetAttribute(p0, p2 + 1)) & 0x1C) != 0) {
                s0 = (s0 & 0xFFFF) - 3;
            }
        } else if ((p0 & 4) != 0) {
            s0 = -5;
            if ((p1 & 8) != 0) {
                s0 = -3;
            }
            put8(g_18a2b, (u8(g_186ff) & 3) + 9);
            p2 = (s16(g_1870a) + 8) >>> 3;
            p0 = ((s16(g_18708) + (short) s0 + 8) >>> 3) & 0xFF;
            p3 = vMapGetAttribute(p0, p2) & 0xFF;
            if (((p3 | vMapGetAttribute(p0, p2 + 1)) & 0x1C) != 0) {
                s0 = (s0 & 0xFFFF) + 3;
            }
        }
        p1 = u8(fp + 4);
        if ((p1 & 6) != 0) {
            B_142b0: {
                B_142a8: {
                    put16(fp + 16, 0);
                    p0 = i32(g_19118);
                    if ((p0 & 0x101) == 0) {
                        if ((p0 & 2) != 0) {
                            if ((short) s0 == 0) {
                                r0 = u8(fp + 7) | 7;
                                break B_142a8;
                            }
                        }
                        if (p0 == 0) {
                            if ((vMapGetAttribute((s16(g_18708) + 8) >>> 3, (s16(g_1870a) + 8) >>> 3) & 0xC0) != 0) {
                                r0 = u8(g_18702);
                                if ((byte) r0 <= 38) {
                                    put8(g_18702, r0 + 1);
                                    put8(fp + 7, u8(fp + 7) | 6);
                                    fun_11d64();
                                    break B_142b0;
                                }
                                break B_142b0;
                            }
                            break B_142b0;
                        }
                        break B_142b0;
                    }
                    put16(fp + 16, 0xFFFF9000);
                    put8(fp + 4, p1 & 0xF9);
                    put16(g_1870a, u16(g_1870a) - 7);
                    r0 = u8(fp + 7) | 5;
                }
                put8(fp + 7, r0);
            }
            put16(g_18708, u16(g_18708) + s0);
        } else {
            p0 = u16(fp + 16);
            put16(g_1870a, u16(g_1870a) + ((short) p0 >> 12));
            put16(g_18708, u16(g_18708) + s0);
            put8(fp + 7, (u8(fp + 7) & 0xF8) | 4);
            if ((short) p0 <= 0x67FF) {
                put16(fp + 16, p0 + 0x1800);
            }
            if (s16(fp + 16) < 0) {
                put8(fp + 7, u8(fp + 7) | 1);
                p3 = vMapGetAttribute((s16(g_18708) + 6) >> 3, (s16(g_1870a) + 4) >> 3) & 0xFF;
                if (((p3 | vMapGetAttribute((s16(g_18708) + 14) >> 3, (s16(g_1870a) + 4) >> 3)) & 0x1C) != 0) {
                    put16(g_1870a, u16(g_1870c));
                    put16(fp + 16, 0);
                }
            }
        }
        B_142fc: {
            p1 = u16(g_1870a);
            if ((short) p1 >= s16(g_1870c)) {
                if ((u8(fp + 4) & 4) == 0) {
                    put8(fp + 4, 0);
                    if ((p1 & 7) != 4) {
                        if (((s16(g_1870a) + 4) >> 3) == ((s16(g_1870c) + 4) >> 3)) {
                            break B_142fc;
                        }
                    }
                    if ((u8(fp + 7) & 8) == 0) {
                        p3 = u16(g_18708) + 7;
                    } else {
                        p3 = u16(g_18708) + 11;
                    }
                    p1 = ((s16(g_1870a) + 20) >>> 3) & 0xFF;
                    put8(fp + 4, vMapGetAttribute((short) p3 >> 3, p1));
                    r0 = vMapGetAttribute((short) (p3 + 4) >> 3, p1);
                    p0 = u8(fp + 4) | (r0 & 0xFF);
                    put8(fp + 4, p0);
                    if ((p0 & 0x20) == 0) {
                        r0 = p0 & 2;
                        put8(fp + 4, r0);
                        if ((r0 & 0xFF) != 0) {
                            r0 = (u16(g_1870a) - 4) & 0xFFFFFFF8;
                            put16(g_1870a, setB(r0, r0 | 4));
                            break B_142fc;
                        }
                        break B_142fc;
                    }
                    fun_16ecc();
                    break B_142fc;
                }
            }
            put8(fp + 4, 0);
        }
        B_14358: {
            p0 = s16(g_19120);
            p2 = ((p0 + u16(g_186f4)) & 0xFFFF) - 12;
            p3 = ((u16(g_18da4) + u16(g_186f6)) & 0xFFFF) - 6;
            p1 = u16(g_1870e);
            r0 = (short) p1;
            if (r0 == p0) {
                if (s16(g_18708) >= r0 - 6) {
                    break B_14358;
                }
                r0 = (p1 & 0xFFFF) - 6;
            } else {
                if (s16(g_18708) >= -6) {
                    break B_14358;
                }
                r0 = -6;
            }
            put16(g_18708, r0);
        }
        if (s16(g_18708) > (short) p2) {
            put16(g_18708, p2);
        }
        p1 = u16(g_1870a);
        if ((short) p1 > (short) p3) {
            put32(g_19128, fun_125ec);
        }
        p0 = i32(g_19118);
        if ((p0 & 0x10) != 0) {
            p2 = g_18a3c;
            if (i32(p2) == 0) {
                if ((p0 & 2) == 0) {
                    r0 = (p1 & 0xFFFF) + 6;
                } else {
                    r0 = (p1 & 0xFFFF) + 9;
                }
                put16(g_18a48, r0);
                put32(p2, i32(g_1858c));
                if ((u8(fp + 7) & 8) == 0) {
                    put16(g_18a46, u16(g_18708) + 16);
                } else {
                    put16(g_18a46, u16(g_18708));
                }
                put8(g_18a43, 2);
                put32(g_18a50, fun_16364);
                put8(g_18a40, u8(fp + 7) & 8);
                fun_11cc4();
            }
        }
        r0 = fun_13dac();
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14834 @ 0x14834  (address taken)
     */
    int fun_14834(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        B_14868: {
            sp -= 12;
            s0 = p0 & 0xFF;
            fp = (s0 & 0xFF) * 24 + g_18724;
            if (s16(g_1871e) != 0) {
                r0 = s16(g_18722);
                if (r0 != 0) {
                    break B_14868;
                }
            }
            put32(fp, i32(((u8(g_186ff) << 1) & 4) + g_1859c));
            if ((fun_16b84(p0) & 0xFF) != 0) {
                r0 = 0;
                put8(g_18712, r0);
                put8(g_18713, r0);
                put8(g_18718, r0);
                put32(g_19128, fun_12298);
                fun_11d38();
                put32(fp, 0);
            }
            r0 = u16(g_18710);
            vSpriteSet(s0, i32(fp), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - r0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14920 @ 0x14920  (address taken)
     */
    int fun_14920(int p0) {
        int p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, r0 = 0;
        B_149a4: {
            sp -= 4;
            g2 = p0 & 0xFF;
            g0 = (g2 & 0xFF) * 24 + g_18724;
            p0 = u16(g0 + 10);
            p3 = (p0 & 0xFFFF) - u16(g_1870e);
            r0 = u16(g0 + 12);
            g1 = (r0 & 0xFFFF) - u16(g_18710);
            put16(g0 + 12, r0 - 1);
            put16(g0 + 10, p0 - 1);
            p2 = u8(g_186ff) & 1;
            r0 = u8(g0 + 7);
            if ((r0 & 0xFF) == 5) {
                p1 = g1 & 0xFFFF;
                vPlot(p3, p1);
                vPlot(p3 + p2 + 6, p1);
                p2 = (p3 & 0xFFFF) + ((p2 << 1) & 0xFFFF);
                vPlot(p2 + 16, p1);
                p0 = (p3 & 0xFFFF) + 4;
                p1 = (g1 & 0xFFFF) + 2;
            } else {
                if ((r0 & 0xFF) != 4) {
                    put32(g0, 0);
                    break B_149a4;
                }
                p1 = (g1 & 0xFFFF) - 2;
                vPlot(p3, p1);
                p2 = (p3 & 0xFFFF) + ((p2 << 1) & 0xFFFF);
                vPlot(p2 + 18, p1);
                p0 = (p3 & 0xFFFF) + 4;
                p1 = (g1 & 0xFFFF) + 1;
            }
            vPlot(p0, p1);
            vPlot(p2 + 12, p1);
        }
        vSpriteSet(g2, 0, 0, 0);
        r0 = u8(g_186ff);
        r0 = setB(r0, r0 & 1);
        if ((r0 & 0xFF) != 0) {
            r0 = setB(r0, u8(g0 + 7) + 255);
            put8(g0 + 7, r0);
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_14a74 @ 0x14a74  (address taken)
     */
    int fun_14a74(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        put32(fp, i32((u8(g_186ff) & 4) + g_18594));
        r0 = fun_16b84(p0);
        if ((r0 & 0xFF) != 0) {
            put8(fp + 7, 5);
            put16(g_18706, u16(g_18706) + u16(fp + 8));
            put32(fp + 20, fun_14920);
            r0 = fun_11c18();
        }
        vSpriteSet(s0, i32(fp), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14b3c @ 0x14b3c  (address taken)
     */
    int fun_14b3c(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        if ((u8(g_186ff) & 1) != 0) {
            put8(fp + 7, u8(fp + 7) + 1);
        }
        if (u8(fp + 7) > 2) {
            put8(fp + 7, 0);
        }
        put32(fp, i32((u8(fp + 7) << 2) + g_185a4));
        if ((fun_16b84(p0) & 0xFF) != 0) {
            p0 = g_18703;
            put8(p0, u8(p0) + 1);
            put32(fp + 20, fun_14920);
            put8(fp + 7, 5);
            fun_11bd8();
        }
        r0 = u16(g_1870e);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14c3c @ 0x14c3c  (address taken)
     */
    int fun_14c3c(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        r0 = u8(fp + 7) + 1;
        put8(fp + 7, r0);
        put32(fp, i32(((r0 & 1) << 2) + g_18584));
        if ((u8(fp + 4) & 0x10) == 0) {
            r0 = u16(fp + 10) - 1;
        } else {
            r0 = u16(fp + 10) + 1;
        }
        put16(fp + 10, r0);
        if ((vMapGetAttribute((s16(fp + 10) + u8(fp + 4)) >> 3, (s16(fp + 12) >> 3) + 1) & 2) == 0) {
            put8(fp + 4, u8(fp + 4) ^ 0x10);
        }
        fun_16854(s0);
        r0 = u16(g_1870e);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14d68 @ 0x14d68  (address taken)
     */
    int fun_14d68(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        r0 = u16(fp + 10) - 3;
        put16(fp + 10, r0);
        if ((short) r0 - s16(g_1870e) >= -16) {
            r0 = u8(fp + 7) ^ 1;
            put8(fp + 7, r0);
            put32(fp, i32(((r0 & 0xFF) << 2) + g_1857c));
            fun_16854(p0);
            r0 = u16(g_1870e);
            vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        } else {
            r0 = 0;
            put32(fp, r0);
            vSpriteSet(p0, 0, 0, 0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14e50 @ 0x14e50  (address taken)
     */
    int fun_14e50(int p0) {
        int fp = 0, s0 = 0, p1 = 0, p2 = 0, r0 = 0;
        B_14ec4: {
            sp -= 12;
            s0 = p0 & 0xFF;
            fp = (s0 & 0xFF) * 24 + g_18724;
            r0 = u16(fp + 10) + 1;
            put16(fp + 10, r0);
            p1 = (short) r0 - s16(g_1870e);
            r0 = u16(g_186f4) + 20;
            if (p1 <= r0) {
                if (p1 >= -50) {
                    r0 = u8(fp + 7) + 255;
                    put8(fp + 7, r0);
                    if ((r0 & 0xFF) == 255) {
                        put8(fp + 7, 16);
                        put16(fp + 16, -u16(fp + 16));
                    }
                    put32(fp, i32(((u8(fp + 7) << 1) & 4) + g_18574));
                    put16(fp + 12, u16(fp + 12) + u16(fp + 16));
                    fun_16854(p0);
                    r0 = u16(g_1870e);
                    vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
                    break B_14ec4;
                }
            }
            p2 = 0;
            put32(fp, p2);
            vSpriteSet(p0, 0, p2, 0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_14f94 @ 0x14f94  (address taken)
     */
    int fun_14f94(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, p1 = 0, r0 = 0;
        B_15000: {
            sp -= 20;
            s0 = p0 & 0xFF;
            s2 = 0;
            p1 = 2;
            fp = (s0 & 0xFF) * 24 + g_18724;
            if ((u8(g_186ff) & 1) == 0) {
                if ((u8(fp + 4) & 8) == 0) {
                    s2 = 5;
                    p1 = -2;
                }
                r0 = u8(fp + 5);
                if ((r0 & 0xFF) == 0) {
                    put8(fp + 7, (u8(fp + 7) & 1) ^ 1);
                    s1 = (byte) p1;
                    r0 = (s1 & 0xFFFF) + u16(fp + 10);
                    put16(fp + 10, r0);
                    if ((vMapGetAttribute((((short) r0 + u8(fp + 4)) >> 3) + 1, (s16(fp + 12) >> 3) + 3) & 2) == 0) {
                        put8(fp + 4, u8(fp + 4) ^ 8);
                    }
                    if (((u16(g_1870a) - u16(fp + 12) + 12) & 0xFFFF) <= 27) {
                        if ((u8(fp + 4) & 8) == 0) {
                            r0 = ((u16(g_18708) - u16(fp + 10)) & 0xFFFF) + 68;
                        } else {
                            r0 = ((u16(g_18708) - u16(fp + 10)) & 0xFFFF) + 16;
                        }
                        if ((r0 & 0xFFFF) <= 67) {
                            r0 = fun_1270c();
                            if ((s0 & 0xFF) == 0) {
                                break B_15000;
                            }
                            p0 = (r0 & 0xFF) * 24;
                            put32(p0 + g_18724, i32(g_1858c));
                            put32(p0 + g_18738, fun_152a4);
                            p1 = p0 + g_1872c;
                            put16(p1 + 2, u16(fp + 10) + (u8(fp + 4) << 1));
                            put16(p0 + g_18730, u16(fp + 12) + 12);
                            p0 += g_18728;
                            put8(p0, u8(fp + 4));
                            put8(p0 + 3, 22);
                            put16(p1, 15);
                            put8(fp + 5, 8);
                            put8(fp + 7, 4);
                            put16(fp + 10, u16(fp + 10) - s1);
                        }
                    }
                } else {
                    put8(fp + 5, r0 + 255);
                    put8(fp + 7, 4);
                }
                put32(fp, i32(((u8(fp + 7) + (s2 & 0xFF)) << 2) + g_1854c));
                fun_16a10(s0);
                r0 = u16(g_1870e);
                vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
            } else {
                r0 = u16(g_18710);
                vSpriteSet(p0, i32(fp), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - r0);
            }
        }
        sp += 20;
        return r0;
    }
    
    /**
     * fun_152a4 @ 0x152a4  (address taken)
     */
    int fun_152a4(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        r0 = u8(fp + 7) + 255;
        put8(fp + 7, r0);
        if ((r0 & 0xFF) == 255) {
            put32(fp, 0);
        } else {
            if ((u8(fp + 4) & 8) == 0) {
                r0 = u16(fp + 10) - 4;
            } else {
                r0 = u16(fp + 10) + 4;
            }
            put16(fp + 10, r0);
            fun_16934(s0);
        }
        r0 = u16(g_1870e);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_15370 @ 0x15370  (address taken)
     */
    int fun_15370(int p0) {
        int fp = 0, s0 = 0, s1 = 0, r0 = 0;
        sp -= 16;
        s1 = p0 & 0xFF;
        fp = (s1 & 0xFF) * 24 + g_18724;
        s0 = fun_16b84(p0) & 0xFF;
        if (u8(fp + 7) == 0) {
            if ((s0 & 0xFF) != 0) {
                p0 = u8(fp + 4) ^ 1;
                put8(fp + 4, p0);
                put8(u8(fp + 5) * 24 + g_18728, p0);
                fun_11d04();
            }
        }
        put8(fp + 7, s0);
        r0 = (u8(fp + 4) << 2) + g_18530;
        put32(fp, i32(r0));
        vSpriteSet(s1, i32(r0), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - u16(g_18710));
        sp += 16;
        return r0;
    }
    
    /**
     * fun_15450 @ 0x15450  (address taken)
     */
    int fun_15450(int p0) {
        int fp = 0, s0 = 0, p1 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, r0 = 0;
        sp -= 20;
        s0 = p0 & 0xFF;
        g3 = 0;
        p3 = 0;
        g4 = s0 & 0xFF;
        r0 = g4 * 24;
        fp = r0 + g_18724;
        g2 = g_18a24;
        p0 = u8(fp + 4);
        r0 = setB(r0, p0 & 4);
        if ((r0 & 0xFF) == 0) {
            if ((p0 & 0xFF) != 0) {
                r0 = u8(fp + 7) + 254;
                put8(fp + 7, r0);
                g3 = u16(fp + 14);
                put16(fp + 10, u16(fp + 10) + g3);
                p1 = u16(fp + 16);
                put16(fp + 12, u16(fp + 12) + p1);
                if ((r0 & 0xFF) == 0) {
                    put8(fp + 7, u8(fp + 5) << 3);
                    put16(fp + 14, -g3);
                    put16(fp + 16, -p1);
                }
            }
            if (s16(g2 + 16) >= 0) {
                p0 = u16(g_18708) - u16(g_1870e);
                put16(sp, p0 + 4);
                put16(sp + 2, u16(g_1870a) - u16(g_18710) + 5);
                put16(sp + 4, 12);
                put16(sp + 6, 16);
                if ((u8(g2 + 7) & 8) != 0) {
                    put16(sp, p0 + 8);
                }
                g1 = sp;
                p3 = vSpriteBoxCollision(g1, s0, s0) & 0xFF;
                g0 = (byte) s0;
                if ((byte) p3 != g0) {
                    vSpriteSet(s0, i32(fp), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - u16(g_18710));
                    p3 = vSpriteBoxCollision(g1, s0, s0) & 0xFF;
                }
                if ((byte) p3 == g0) {
                    p3 = 0;
                    if (s16(g_1870c) + 14 < s16(fp + 12)) {
                        p0 = u16(sp);
                        put16(sp, p0 + 2);
                        if ((u8(g2 + 7) & 8) != 0) {
                            put16(sp, p0 + 3);
                        }
                        put16(sp + 2, u16(sp + 2) + 15);
                        put16(sp + 6, 1);
                        put16(sp + 4, 7);
                        if ((short) vSpriteBoxCollision(g1, s0, s0) == g4) {
                            put8(g2 + 4, u8(g2 + 4) | 4);
                            p3 = s0 & 0xFF;
                        }
                    }
                }
            }
            if ((byte) p3 == (byte) s0) {
                put16(g_18708, u16(g_18708) + g3);
                put16(g_1870a, u16(fp + 12) - 20);
                fun_13dac();
                put16(g_1870c, u16(g_1870a) - 1);
            }
            r0 = u16(g_1870e);
            vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        }
        sp += 20;
        return r0;
    }
    
    /**
     * fun_15790 @ 0x15790
     * called from: fun_12a60
     */
    void fun_15790() {
        int p0 = 0, r0 = 0;
        sp -= 4;
        p0 = s16(g_19120);
        r0 = div((p0 - s16(g_1870e)) << 16, p0);
        vDrawObject(((u16(g_186f4) - 20) * r0) >> 16, u16(g_18bc8), i32(g_18bbc));
        sp += 4;
    }
    
    /**
     * fun_157e0 @ 0x157e0  (address taken)
     */
    int fun_157e0(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        B_1588c: {
            sp -= 12;
            s0 = p0 & 0xFF;
            fp = (s0 & 0xFF) * 24 + g_18724;
            fun_11c84();
            if ((u8(g_186ff) & 1) != 0) {
                r0 = u8(fp + 7) + 255;
                put8(fp + 7, r0);
                if ((r0 & 0xFF) == 0) {
                    r0 = fun_125ec;
                    put32(g_19128, r0);
                    vSpriteSet(32, 0, 0, 0);
                    break B_1588c;
                }
            }
            r0 = (u8(fp + 7) << 2) + g_1850c;
            put32(fp, i32(r0));
            vSpriteSet(s0, i32(r0), u16(g_18708) - u16(g_1870e), u16(g_1870a) - u16(g_18710));
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_158c0 @ 0x158c0  (address taken)
     */
    int fun_158c0(int p0) {
        int fp = 0, s0 = 0, p1 = 0, p2 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        if (u8(fp + 5) == 0) {
            p1 = u16(fp + 14);
            put16(fp + 10, u16(fp + 10) + ((short) p1 >> 8));
            p2 = u16(fp + 16);
            put16(fp + 12, u16(fp + 12) + ((short) p2 >> 8));
            p0 = u8(fp + 4) << 2;
            put16(fp + 14, (p1 & 0xFF) + (s16(p0 + g_22d70) << 1));
            put16(fp + 16, (p2 & 0xFF) + (s16(p0 + g_22d72) << 1));
        }
        if ((u8(g_186ff) & 4) != 0) {
            r0 = u16(fp + 12) + 1;
        } else {
            r0 = u16(fp + 12) - 1;
        }
        put16(fp + 12, r0);
        r0 = u8(fp + 5);
        if ((r0 & 0xFF) == 0) {
            if (s16((u8(fp + 4) << 2) + g_22d70) < 0) {
                put8(fp + 7, ((u8(g_186ff) >>> 1) & 1) + 3);
                if (s16(fp + 10) <= s16(g_1871a)) {
                    put8(fp + 5, 1);
                    put8(fp + 4, 0);
                }
            } else {
                put8(fp + 7, (u8(g_186ff) >>> 1) & 1);
                if (s16(fp + 10) >= s16(g_1871a)) {
                    put8(fp + 4, 0);
                    put8(fp + 5, 1);
                }
            }
        } else {
            B_15970: {
                put8(fp + 5, r0 + 1);
                if ((u8(g_186ff) & 1) != 0) {
                    if (u8(g_18715) == 0) {
                        r0 = u8(fp + 7);
                        if ((r0 & 0xFF) > 1) {
                            if ((r0 & 0xFF) <= 3) {
                                put8(fp + 7, 5 - (-((r0 ^ 2) & 0xFF) >>> 31));
                                break B_15970;
                            }
                            put8(fp + 7, 3);
                            break B_15970;
                        }
                        r0 = 2;
                    } else {
                        r0 = u8(fp + 7);
                        if ((r0 & 0xFF) > 2) {
                            if ((r0 & 0xFF) != 5) {
                                put8(fp + 7, 5);
                                break B_15970;
                            }
                            r0 = 2;
                        } else {
                            r0 = (r0 ^ 1) & 1;
                        }
                    }
                    put8(fp + 7, r0);
                }
            }
            if (u8(fp + 5) > 30) {
                if (u8(g_18715) == 0) {
                    put8(g_18715, 1);
                    put16(g_1871a, u16(g_18708) - 30);
                    r0 = u16(g_1870a) - 10;
                } else {
                    put8(g_18715, 0);
                    put16(g_1871a, u16(g_1871e));
                    r0 = u16(g_18720);
                }
                put16(g_1871c, r0);
                put8(fp + 5, 0);
                put8(fp + 4, fun_12460(u16(fp + 10), u16(fp + 12), u16(g_1871a), u16(g_1871c)));
            }
        }
        put32(fp, i32((u8(fp + 7) << 2) + g_185b0));
        fun_16af8(s0);
        r0 = u16(g_1870e);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_15c78 @ 0x15c78  (address taken)
     */
    int fun_15c78(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, r0 = 0;
        sp -= 20;
        s0 = p0 & 0xFF;
        s1 = 0;
        s2 = 2;
        fp = (s0 & 0xFF) * 24 + g_18724;
        if ((u8(fp + 4) & 8) == 0) {
            s1 = 4;
            s2 = -2;
        }
        r0 = u8(fp + 5);
        if ((r0 & 0xFF) == 0) {
            if ((u16(g_18da0) & 0x3F) == 63) {
                r0 = fun_1270c();
                if ((r0 & 0xFF) != 0) {
                    p0 = (r0 & 0xFF) * 24 + g_18724;
                    put32(p0, i32(g_18584));
                    put32(p0 + 20, fun_14c3c);
                    put16(p0 + 10, u16(fp + 10) - u8(fp + 4) + 24);
                    put16(p0 + 12, u16(fp + 12) + 8);
                    put8(p0 + 7, 0);
                    put8(p0 + 4, u8(fp + 4) & 0x10);
                    put16(p0 + 8, 12);
                }
            }
            p0 = u8(g_186ff);
            put8(fp + 7, (((p0 - divu(p0, 6) * 6) & 0xFF) >>> 1) + s1);
            r0 = ((byte) s2 & 0xFFFF) + u16(fp + 10);
            put16(fp + 10, r0);
            if ((vMapGetAttribute(((short) r0 + u8(fp + 4)) >> 3, (s16(fp + 12) >> 3) + 2) & 2) == 0) {
                put8(fp + 4, u8(fp + 4) ^ 0x18);
                put8(fp + 7, s1 + 3);
                put8(fp + 5, 2);
            }
        } else {
            r0 = (r0 & 0xFF) + 255;
            put8(fp + 5, r0);
            if ((r0 & 0xFF) == 0) {
                put8(fp + 7, s1 + 3);
            }
        }
        fun_16af8(s0);
        r0 = (u8(fp + 7) << 2) + g_185c8;
        put32(fp, i32(r0));
        vSpriteSet(s0, i32(r0), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - u16(g_18710));
        sp += 20;
        return r0;
    }
    
    /**
     * fun_15ecc @ 0x15ecc  (address taken)
     */
    int fun_15ecc(int p0) {
        int fp = 0, s0 = 0, p1 = 0, p2 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        p1 = 0;
        p2 = 3;
        fp = (s0 & 0xFF) * 24 + g_18724;
        if ((u8(fp + 4) & 0x20) == 0) {
            p1 = p2 & 0xFF;
            p2 = -3;
        }
        r0 = u8(fp + 5);
        if ((r0 & 0xFF) == 0) {
            p0 = u8(g_186ff);
            put8(fp + 7, (((p0 - divu(p0, 5) * 5) & 0xFF) >>> 1) + p1);
            r0 = ((byte) p2 & 0xFFFF) + u16(fp + 10);
            put16(fp + 10, r0);
            if ((vMapGetAttribute(((short) r0 + u8(fp + 4)) >> 3, (s16(fp + 12) >> 3) + 2) & 2) == 0) {
                put8(fp + 4, u8(fp + 4) ^ 0x20);
                put8(fp + 5, 20);
            }
        } else {
            put8(fp + 5, r0 + 255);
            put8(fp + 7, p1);
        }
        fun_16af8(s0);
        r0 = (u8(fp + 7) << 2) + g_185e8;
        put32(fp, i32(r0));
        vSpriteSet(s0, i32(r0), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_16040 @ 0x16040  (address taken)
     */
    int fun_16040(int p0) {
        int fp = 0, s0 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        r0 = u8(fp + 4) + 255;
        put8(fp + 4, r0);
        if ((r0 & 0xFF) == 0) {
            B_16198: {
                r0 = u8(fp + 7);
                if ((r0 & 0xFF) == 24) {
                    put8(fp + 7, 1);
                } else {
                    if ((r0 & 0xFF) == 15) {
                        r0 = 0;
                    } else if ((r0 & 0xFF) == 7) {
                        if ((u16(g_18da0) & 3) == 3) {
                            put8(fp + 7, 16);
                            break B_16198;
                        }
                        r0 = 8;
                    } else {
                        if ((r0 & 0xFF) == 25) {
                            put8(fp + 7, 8);
                            break B_16198;
                        }
                        r0 = (r0 & 0xFF) + 1;
                    }
                    put8(fp + 7, r0);
                }
            }
            put8(fp + 4, u8(u8(fp + 7) * 3 + g_18c08));
        }
        if ((u8(g_186ff) & 4) == 0) {
            r0 = u16(fp + 12) - 1;
        } else {
            r0 = u16(fp + 12) + 1;
        }
        put16(fp + 12, r0);
        p0 = u8(fp + 7) * 3;
        put16(fp + 10, s8(p0 + g_18c06) + u16(fp + 10));
        put16(fp + 12, s8(p0 + g_18c07) + u16(fp + 12));
        r0 = u8(g_186ff);
        r0 = ((((r0 - divu(r0, 6) * 6) & 0xFF) >>> 1) & 0xFF) << 2;
        put32(fp, i32(r0 + g_18600));
        fun_16af8(s0);
        r0 = u16(g_18710);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - u16(g_1870e), u16(fp + 12) - r0);
        sp += 12;
        return r0;
    }
    
    /**
     * fun_16224 @ 0x16224  (address taken)
     */
    int fun_16224(int p0) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, p1 = 0, r0 = 0;
        sp -= 24;
        fp = p0 & 0xFF;
        p0 = (fp & 0xFF) * 24 + g_18724;
        r0 = u8(g_18716) + 255;
        put8(g_18716, r0);
        if ((r0 & 0xFF) == 0) {
            r0 = 0;
            put32(p0, r0);
        } else {
            r0 = i32(p0);
            s1 = s16(r0 + 8);
            s2 = u16(p0 + 12);
            s3 = u16(p0 + 10);
            p0 = u16(g_18da0);
            s0 = s16(r0 + 6);
            p0 -= div(p0, s0) * s0;
            r0 = vGetRandom();
            put16(g_18da0, r0);
            p1 = r0 & 0xFFFF;
            p1 = (s2 & 0xFFFF) + ((p1 - div(p1, s1) * s1) & 0xFFFF);
            fun_140b4((p0 & 0xFF) + s3, p1);
            r0 = vGetRandom();
            put16(g_18da0, r0);
            p0 = r0 & 0xFFFF;
            p0 -= div(p0, s0) * s0;
            r0 = vGetRandom();
            put16(g_18da0, r0);
            p1 = r0 & 0xFFFF;
            p1 = (s2 + (p1 - div(p1, s1) * s1)) & 0xFFFF;
            r0 = fun_140b4((p0 & 0xFF) + s3, p1);
            vSpriteSet(fp, 0, 0, 0);
        }
        sp += 24;
        return r0;
    }
    
    /**
     * fun_16364 @ 0x16364  (address taken)
     */
    int fun_16364(int p0) {
        int fp = 0, s0 = 0, p1 = 0, r0 = 0;
        sp -= 12;
        s0 = p0 & 0xFF;
        fp = (s0 & 0xFF) * 24 + g_18724;
        put32(fp, i32(g_18590));
        if (u8(fp + 7) == 0) {
            put32(fp, 0);
        }
        put8(fp + 7, 0);
        p1 = u16(fp + 10) - u16(g_1870e);
        put16(g_18bf4, p1 + 6);
        if ((u8(fp + 4) & 8) != 0) {
            put16(g_18bf4, p1 - 50);
        }
        put16(g_18bf6, u16(fp + 12) - u16(g_18710) + 2);
        fun_16580();
        r0 = u16(g_1870e);
        vSpriteSet(s0, i32(fp), u16(fp + 10) - r0, u16(fp + 12) - u16(g_18710));
        sp += 12;
        return r0;
    }
    
    /**
     * fun_16460 @ 0x16460  (address taken)
     */
    int fun_16460(int p0) {
        int p1 = 0, p2 = 0, p3 = 0, r0 = 0;
        sp -= 4;
        r0 = (p0 & 0xFF) * 24 + g_18724;
        p1 = u8(r0 + 7) + 255;
        put8(r0 + 7, p1);
        p1 = ((p1 & 0xFF) << 2) + g_184fc;
        put32(r0, i32(p1));
        p2 = u16(r0 + 10) - u16(g_1870e);
        p3 = u16(r0 + 12);
        r0 = u16(g_18710);
        vSpriteSet(p0, i32(p1), p2, p3 - r0);
        sp += 4;
        return r0;
    }
    
    /**
     * fun_164f0 @ 0x164f0  (address taken)
     */
    int fun_164f0(int p0) {
        int p1 = 0, p2 = 0, p3 = 0, r0 = 0;
        sp -= 4;
        r0 = (p0 & 0xFF) * 24 + g_18724;
        p1 = u8(r0 + 7) + 255;
        put8(r0 + 7, p1);
        p1 = ((p1 & 0xFF) << 2) + g_1850c;
        put32(r0, i32(p1));
        p2 = u16(r0 + 10) - u16(g_1870e);
        p3 = u16(r0 + 12);
        r0 = u16(g_18710);
        vSpriteSet(p0, i32(p1), p2, p3 - r0);
        sp += 4;
        return r0;
    }
    
    /**
     * fun_16580 @ 0x16580
     * called from: fun_16364
     */
    void fun_16580() {
        int fp = 0, p0 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, r0 = 0;
        sp -= 8;
        fp = 33;
        while (true) {
            fp = vSpriteBoxCollision(g_18bf4, fp + 1, 47) & 0xFF;
            if ((fp & 0xFF) == 255) {
                sp += 8;
                return;
            }
            p3 = (fp & 0xFF) * 24 + g_18724;
            r0 = i32(p3 + 20);
            if (r0 != fun_164f0) {
                if (r0 != fun_16460) {
                    if (r0 != fun_152a4) {
                        p0 = 1;
                        p2 = i32(p3);
                        if (u16(p2 + 8) <= 16) {
                            p0 = 2;
                        }
                        r0 = p0 & 3;
                        p1 = u16(p3 + 8) - (r0 & 0xFFFF);
                        put16(p3 + 8, p1);
                        put16(g_18706, u16(g_18706) + r0);
                        if ((byte) fp == s8(g_18718)) {
                            if ((short) p1 == 0) {
                                put16(g_18722, 0);
                                put32(p3 + 20, fun_16224);
                                put8(g_18716, 15);
                                fun_11c58();
                            } else {
                                p1 = ((u16(g_18bf6) + u16(g_18710)) & 0xFFFF) - 2;
                                p0 = u16(g_18da0);
                                if ((p0 & 4) == 0) {
                                    p1 = (p1 & 0xFFFF) - (p0 & 3);
                                } else {
                                    p1 = (p1 & 0xFFFF) + (p0 & 3);
                                }
                                p2 = u16(p3 + 10);
                                if (u8(g_18a40) != 0) {
                                    p0 = i32(p3);
                                    p2 = ((p2 + (u16(p0 + 6) - u16(p0 + 2))) & 0xFFFF) - 3;
                                }
                                fun_14014(p2, p1);
                            }
                        } else if ((short) p1 == 0) {
                            if (u16(p2 + 8) > 8) {
                                put32(p3, i32(g_18520));
                                put8(p3 + 7, 6);
                                put32(p3 + 20, fun_164f0);
                                fun_11c58();
                            } else {
                                put32(p3, i32(g_18508));
                                put8(p3 + 7, 4);
                                put32(p3 + 20, fun_16460);
                            }
                        } else {
                            p1 = ((u16(g_18bf6) + u16(g_18710)) & 0xFFFF) - 2;
                            p0 = u16(g_18da0);
                            if ((p0 & 4) == 0) {
                                p1 = (p1 & 0xFFFF) - (p0 & 1);
                            } else {
                                p1 = (p1 & 0xFFFF) + (p0 & 1);
                            }
                            p2 = u16(p3 + 10);
                            g0 = g_18a40;
                            if (u8(g0) != 0) {
                                p0 = i32(p3);
                                p2 = ((p2 + (u16(p0 + 6) - u16(p0 + 2))) & 0xFFFF) - 16;
                            }
                            fun_14014(u8(g0) + p2, p1);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * fun_16854 @ 0x16854
     * called from: fun_14c3c, fun_14d68, fun_14e50
     */
    void fun_16854(int p0) {
        int fp = 0, p3 = 0, r0 = 0;
        sp -= 8;
        p3 = p0 & 0xFF;
        if ((vSpriteBoxCollision(g_18bec, p3, p3) & 0xFF) != 255) {
            fp = (p3 & 0xFF) * 24 + g_18724;
            if (i32(g_18a38) != fun_157e0) {
                vSysCtl(1, 0x12C0001);
                r0 = g_18702;
                p0 = (s8(r0) & 0xFF) - (s8(fp + 8) & 0xFF);
                put8(r0, p0);
                if ((byte) p0 <= 0) {
                    fun_16ecc();
                }
                put32(fp, i32(g_18508));
                put8(fp + 7, 4);
                put32(fp + 20, fun_16460);
            }
        }
        sp += 8;
    }
    
    /**
     * fun_16934 @ 0x16934
     * called from: fun_152a4
     */
    void fun_16934(int p0) {
        int fp = 0, p3 = 0, r0 = 0;
        sp -= 8;
        p3 = p0 & 0xFF;
        if ((vSpriteCollision(32, p3, p3) & 0xFF) != 255) {
            fp = (p3 & 0xFF) * 24 + g_18724;
            if (i32(g_18a38) != fun_157e0) {
                vSysCtl(1, 0x12C0001);
                r0 = g_18702;
                p0 = (s8(r0) & 0xFF) - (s8(fp + 8) & 0xFF);
                put8(r0, p0);
                if ((byte) p0 <= 0) {
                    fun_16ecc();
                }
                put32(fp, i32(g_18508));
                put8(fp + 7, 4);
                put32(fp + 20, fun_16460);
            }
        }
        sp += 8;
    }
    
    /**
     * fun_16a10 @ 0x16a10
     * called from: fun_14f94
     */
    void fun_16a10(int p0) {
        int fp = 0, p3 = 0, r0 = 0;
        sp -= 8;
        p3 = p0 & 0xFF;
        if ((vSpriteBoxCollision(g_18bec, p3, p3) & 0xFF) != 255) {
            fp = (p3 & 0xFF) * 24 + g_18724;
            if (i32(g_18a38) != fun_157e0) {
                vSysCtl(1, 0x12C0001);
                r0 = g_18702;
                p0 = (s8(r0) & 0xFF) - (s8(fp + 8) & 0xFF);
                put8(r0, p0);
                if ((byte) p0 <= 0) {
                    fun_16ecc();
                }
                put32(fp, i32(g_18520));
                put8(fp + 7, 6);
                put32(fp + 20, fun_164f0);
                fun_11c58();
            }
        }
        sp += 8;
    }
    
    /**
     * fun_16af8 @ 0x16af8
     * called from: fun_158c0, fun_15c78, fun_15ecc, fun_16040
     */
    void fun_16af8(int p0) {
        int p1 = 0, r0 = 0;
        sp -= 4;
        p1 = p0 & 0xFF;
        if ((vSpriteBoxCollision(g_18bec, p1, p1) & 0xFF) != 255) {
            if (i32(g_18a38) != fun_157e0) {
                vSysCtl(1, 0x12C0001);
                r0 = g_18702;
                p0 = u8(r0) + 255;
                put8(r0, p0);
                if ((byte) p0 <= 0) {
                    fun_16ecc();
                }
            }
        }
        sp += 4;
    }
    
    /**
     * fun_16b84 @ 0x16b84
     * called from: fun_14834, fun_14a74, fun_14b3c, fun_15370
     */
    int fun_16b84(int p0) {
        int p1 = 0, r0 = 0;
        sp -= 4;
        p1 = p0 & 0xFF;
        r0 = -((vSpriteBoxCollision(g_18bec, p1, p1) ^ 0xFF) & 0xFF) >>> 31;
        sp += 4;
        return r0;
    }
    
    /**
     * fun_16bbc @ 0x16bbc  (address taken)
     */
    int fun_16bbc() {
        int s0 = 0, p0 = 0, p1 = 0, r0 = 0;
        sp -= 12;
        r0 = fun_1270c();
        if ((r0 & 0xFF) != 0) {
            s0 = (r0 & 0xFF) * 24 + g_18724;
            put8(g_18718, r0);
            p0 = ((u16(g_1870e) + u16(g_186f4)) & 0xFFFF) + 40;
            put16(s0 + 10, p0);
            put16(s0 + 12, 80);
            put32(s0, i32(g_185bc));
            put32(s0 + 20, fun_158c0);
            put8(s0 + 4, 1);
            put16(s0 + 8, 100);
            r0 = 0;
            put16(s0 + 14, r0);
            put16(s0 + 16, r0);
            put8(g_18716, 1);
            put8(g_18717, r0);
            put8(g_18715, r0);
            put16(g_1871a, u16(g_1871e));
            put16(g_1871c, u16(g_18720));
            p1 = 0;
            put8(s0 + 7, p1);
            put8(s0 + 5, p1);
            r0 = fun_12460(p0, 80, u16(g_1871e), u16(g_18720));
            put8(s0 + 4, r0);
        }
        sp += 12;
        return r0;
    }
    
    /**
     * fun_16cd8 @ 0x16cd8  (address taken)
     */
    int fun_16cd8() {
        int p0 = 0, r0 = 0;
        sp -= 4;
        r0 = fun_1270c();
        p0 = r0 & 0xFF;
        if ((p0 & 0xFF) != 0) {
            r0 = (p0 & 0xFF) * 24 + g_18724;
            put8(g_18718, p0);
            put16(r0 + 10, u16(g_1870e) + u16(g_186f4));
            put16(r0 + 12, u16(g_18720));
            put32(r0, i32(g_185d8));
            put32(r0 + 20, fun_15c78);
            p0 = 0;
            put8(r0 + 4, p0);
            put8(r0 + 5, p0);
            put16(r0 + 8, 200);
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_16d78 @ 0x16d78  (address taken)
     */
    int fun_16d78() {
        int p0 = 0, r0 = 0;
        sp -= 4;
        r0 = fun_1270c();
        p0 = r0 & 0xFF;
        if ((p0 & 0xFF) != 0) {
            r0 = (p0 & 0xFF) * 24 + g_18724;
            put8(g_18718, p0);
            put16(r0 + 10, u16(g_1870e) + u16(g_186f4));
            put16(r0 + 12, u16(g_18720));
            put32(r0, i32(g_185f8));
            put32(r0 + 20, fun_15ecc);
            p0 = 0;
            put8(r0 + 4, p0);
            put8(r0 + 5, p0);
            put16(r0 + 8, 300);
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_16e18 @ 0x16e18  (address taken)
     */
    int fun_16e18() {
        int p0 = 0, r0 = 0;
        sp -= 4;
        r0 = fun_1270c();
        p0 = r0 & 0xFF;
        if ((p0 & 0xFF) != 0) {
            r0 = (p0 & 0xFF) * 24 + g_18724;
            put8(g_18718, p0);
            put16(r0 + 10, u16(g_1870e) + u16(g_186f4));
            put16(r0 + 12, u16(g_18720));
            put32(r0, i32(g_18600));
            put32(r0 + 20, fun_16040);
            put8(r0 + 5, 0);
            put16(r0 + 8, 400);
            put8(r0 + 7, 25);
            put8(r0 + 4, u8(g_18c53));
        }
        sp += 4;
        return r0;
    }
    
    /**
     * fun_16ecc @ 0x16ecc
     * called from: fun_14144, fun_16854, fun_16934, fun_16a10, fun_16af8
     */
    void fun_16ecc() {
        sp -= 4;
        put8(g_18702, 0);
        put32(g_18a38, fun_157e0);
        put32(g_18a24, i32(g_18520));
        put8(g_18a2b, 5);
        vSysCtl(1, 0x12C0001);
        fun_11c58();
        sp += 4;
    }
    
    /**
     * fun_16f28 @ 0x16f28
     * called from: fun_11da4, fun_17b94
     */
    void fun_16f28() {
        sp -= 12;
        put16(sp, 8);
        vGetCaps(0, sp);
        vClearScreen(0x80000000);
        vSetTransferMode(1);
        vDrawObject((u16(sp + 4) >>> 1) - 4, (u16(sp + 6) >>> 1) - 7, g_18cc4);
        vFlipScreen(0);
        sp += 12;
    }
    
    /**
     * fun_16fac @ 0x16fac
     * called from: fun_17024, fun_17374
     */
    void fun_16fac(int p0, int p1) {
        int p3 = 0;
        sp -= 4;
        vSetForeColor(p0);
        p3 = p1 & 0xFFFF;
        vTextOutU(u16(g_18d18) + 3, u16(g_18d1a) + u16(g_18d16) + (p3 - u16(g_18d12)) * u16(g_18d14), i32((p3 << 2) + i32(g_18d08)));
        sp += 4;
    }
    
    /**
     * fun_17024 @ 0x17024
     * called from: fun_17374
     */
    void fun_17024(int p0) {
        int fp = 0, s0 = 0, p1 = 0, p2 = 0, p3 = 0, g0 = 0, g1 = 0, g2 = 0, g3 = 0, g4 = 0, g5 = 0, r0 = 0;
        sp -= 12;
        icall(i32(g_18d1c), p0);
        g3 = g_18d04;
        if (i32(g3) != 0) {
            vSetForeColor(0x8000210A);
            g2 = g_18d18;
            p2 = u16(g2);
            g1 = g_18d16;
            g0 = g_18d0c;
            p1 = u16(g1);
            vFillRect(p2 + 2, p1, p2 + u16(g0) - 3, p1 + u16(g_18d1a) - 1);
            vSetForeColor(0x800003FF);
            p2 = u16(g0) - (vTextExtentU(i32(g3)) & 0xFFFF);
            vTextOutU(((p2 + (p2 >>> 31)) >> 1) + u16(g2) + 1, u16(g1) + 2, i32(g3));
        }
        s0 = 0;
        fp = g_18d02;
        p0 = 0;
        while (ltu(p0, u16(fp))) {
            if (geu(p0, u16(g_18d00))) {
                break;
            }
            p2 = g_18d12;
            if (u16(g_18d10) == (s0 & 0xFFFF) + u16(p2)) {
                fun_16fac(0x800003FF, s0 + u16(p2));
            } else {
                fun_16fac(0x80007FFF, s0 + u16(p2));
            }
            s0 = (s0 & 0xFFFF) + 1;
            p0 = s0 & 0xFFFF;
        }
        g5 = g_18d02;
        g0 = u16(g5);
        if ((g0 & 0xFFFF) < u16(g_18d00)) {
            g0 *= u16(g_18d14);
            vSetForeColor(0x80001DEF);
            g4 = g_18d18;
            g3 = g_18d0c;
            p2 = u16(g4) + u16(g3);
            g2 = g_18d16;
            g1 = g_18d1a;
            p3 = u16(g2);
            r0 = u16(g1);
            vFillRect(p2 - 9, p3 + r0 + 2, p2 - 3, p3 + g0 + r0);
            vSetForeColor(0x8000210A);
            p2 = u16(g4) + u16(g3);
            p3 = u16(g2);
            r0 = u16(g1);
            vFillRect(p2 - 8, p3 + r0 + 3, p2 - 4, p3 + g0 + r0 - 1);
            g0 = (g0 & 0xFFFF) - 2;
            p0 = u16(g_18d00);
            p3 = (div(g0 * u16(g5), p0) & 0xFFFF) - 3;
            g0 = div(g0 * u16(g_18d12), p0);
            g0 = ((g0 + u16(g1)) & 0xFFFF) + 2;
            vSetForeColor(0x80007FFF);
            p2 = u16(g4) + u16(g3);
            g0 = u16(g2) + (g0 & 0xFFFF);
            vFillRect(p2 - 7, g0 + 2, p2 - 5, g0 + p3 + 2);
        }
        vFlipScreen(0);
        sp += 12;
    }
    
    /**
     * fun_17374 @ 0x17374
     * called from: fun_11eac, fun_17b94
     */
    int fun_17374(int p0, int p1, int p2, int p3) {
        int fp = 0, s0 = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, s6 = 0, g0 = 0, r0 = 0;
        B_173e4: {
            sp -= 136;
            s1 = p0 & 0xFFFF;
            s3 = p1;
            s2 = p2;
            s5 = p3 & 0xFFFF;
            s6 = u16(sp + 144);
            s4 = u16(sp + 148);
            fp = 0;
            s0 = s1 & 0xFFFF;
            if (s0 != 0) {
                while (true) {
                    fun_178e8(sp, i32(((fp & 0xFFFF) << 2) + s2));
                    fp = (fp & 0xFFFF) + 1;
                    if (geu(fp & 0xFFFF, s0)) {
                        break B_173e4;
                    }
                }
            }
        }
        B_174a8: {
            put16(g_18d00, s1);
            put32(g_18d04, s3);
            put32(g_18d08, s2);
            put16(g_18d0c, u16(sp + 140));
            put16(g_18d0e, s6);
            put16(g_18d10, s4);
            put32(g_18d1c, i32(sp + 152));
            p3 = g_18d14;
            put16(p3, 0);
            fp = 0;
            g0 = s1 & 0xFFFF;
            if (g0 != 0) {
                while (true) {
                    p2 = ((fp & 0xFFFF) << 2) + s2;
                    if (u16(p3) <= (vTextExtentU(i32(p2)) >>> 16)) {
                        r0 = vTextExtentU(i32(p2)) >>> 16;
                    } else {
                        r0 = u16(p3);
                    }
                    put16(p3, r0);
                    fp = (fp & 0xFFFF) + 1;
                    if (geu(fp & 0xFFFF, g0)) {
                        break B_174a8;
                    }
                }
            }
        }
        if (s3 != 0) {
            r0 = ((vTextExtentU(s3) >>> 16) & 0xFFFF) + 2;
        } else {
            r0 = 0;
        }
        put16(g_18d1a, r0);
        p0 = s6 & 0xFFFF;
        p3 = u16(g_18d1a);
        p2 = u16(g_18d14);
        p1 = div(p0 - p3, p2);
        put16(g_18d02, p1);
        put16(g_18d18, s5);
        p0 = p0 - (p1 & 0xFFFF) * p2 - p3;
        put16(g_18d16, u16(sp + 136) + ((p0 + (p0 >>> 31)) >> 1));
        p0 = s4 & 0xFFFF;
        if (ltu(p0, p1 & 0xFFFF)) {
            p0 = 0;
            put16(g_18d12, p0);
        } else {
            put16(g_18d12, s4 - p1 + 1);
        }
        fun_17024(p0);
        s1 = 1;
        while (true) {
            p0 = vGetButtonData();
            if (p0 != 0) {
                if ((s1 & 0xFF) != 0) {
                    sleep();
                    continue;
                }
            }
            B_17758: {
                s1 = 1;
                if ((p0 & 5) != 0) {
                    s0 = g_18d10;
                    r0 = u16(s0);
                    fp = (r0 & 0xFFFF) - 1;
                    if ((r0 & 0xFFFF) == 0) {
                        fp = u16(g_18d00) - 1;
                    }
                    p2 = fp & 0xFFFF;
                    p1 = g_18d12;
                    p3 = g_18d02;
                    p0 = u16(p3);
                    if (p2 >= u16(p1) + p0) {
                        put16(s0, fp);
                        r0 = u16(g_18d00) - u16(p3);
                        break B_17758;
                    }
                    p0 = u16(p1);
                    if (ltu(p2, p0 & 0xFFFF)) {
                        put16(s0, fp);
                        r0 = (p0 & 0xFFFF) - 1;
                        break B_17758;
                    }
                } else {
                    if ((p0 & 0xA) == 0) {
                        if ((p0 & 0x10) == 0) {
                            r0 = -1;
                            s1 = 0;
                            if ((p0 & 0x20) == 0) {
                                continue;
                            }
                        } else {
                            r0 = s16(g_18d10);
                        }
                        sp += 136;
                        return r0;
                    }
                    s0 = g_18d10;
                    p0 = u16(s0) + 1;
                    p1 = u16(g_18d00);
                    fp = p0 - div(p0, p1) * p1;
                    p2 = fp & 0xFFFF;
                    p1 = g_18d12;
                    p0 = u16(g_18d02);
                    if (p2 >= u16(p1) + p0) {
                        put16(s0, fp);
                        p0 = u16(p1);
                        r0 = (p0 & 0xFFFF) + 1;
                        break B_17758;
                    }
                    if (ltu(p2, u16(p1))) {
                        put16(s0, fp);
                        r0 = 0;
                        break B_17758;
                    }
                }
                fun_16fac(0x80007FFF, u16(s0));
                put16(s0, fp);
                fun_16fac(0x800003FF, fp);
                vFlipScreen(0);
                continue;
            }
            put16(p1, r0);
            fun_17024(p0);
        }
    }
    
    /**
     * fun_177cc @ 0x177cc
     * called from: fun_1784c, fun_17b94
     */
    void fun_177cc(int p0, int p1) {
        int r1 = 0;
        sp -= 4;
        while (true) {
            r1 = u16(p1);
            if ((r1 & 0xFFFF) == 0) {
                put16(p0, 0);
                sp += 4;
                return;
            }
            put16(p0, r1);
            p1 += 2;
            p0 += 2;
        }
    }
    
    /**
     * fun_1780c @ 0x1780c
     * called from: fun_1784c, fun_17b94
     */
    int fun_1780c(int p0) {
        int r1 = 0;
        B_17840: {
            sp -= 4;
            r1 = 0;
            if (u16(p0) != 0) {
                while (true) {
                    r1 = (r1 & 0xFFFF) + 1;
                    if (u16(((r1 & 0xFFFF) << 1) + p0) == 0) {
                        break B_17840;
                    }
                }
            }
        }
        sp += 4;
        return r1 & 0xFFFF;
    }
    
    /**
     * fun_1784c @ 0x1784c
     * called from: fun_10768
     */
    void fun_1784c(int p0, int p1) {
        sp -= 16;
        p0 += ((fun_1780c(p0) & 0xFFFF) << 1);
        fun_177cc(p0, p1);
        sp += 16;
    }
    
    /**
     * fun_178e8 @ 0x178e8
     * called from: fun_17374
     */
    void fun_178e8(int p0, int p1) {
        int p2 = 0, r1 = 0;
        sp -= 4;
        p2 = p0;
        r1 = 0;
        while (true) {
            p0 = u8((r1 << 1) + p1);
            put8(p2 + r1, p0);
            r1++;
            if ((p0 & 0xFF) == 0) {
                sp += 4;
                return;
            }
        }
    }
    
    /**
     * fun_17960 @ 0x17960
     * called from: fun_17a14
     */
    void fun_17960() {
    }
    
    /**
     * fun_17968 @ 0x17968
     * called from: fun_17a14, fun_17b94
     */
    int fun_17968(int p0) {
        int p3 = 0, g0 = 0, g1 = 0, g2 = 0, r0 = 0;
        sp -= 8;
        g2 = p0;
        g1 = 0;
        g0 = g_22e0c;
        p3 = g_22e10;
        while (true) {
            if (vStreamRead(g2, sp + 2, 2) == 0) {
                put16(p3, 0);
                r0 = g1;
                if (g1 == 0) {
                    break;
                }
                r0 = 1;
                break;
            }
            p0 = u16(sp + 2);
            put16(p3, p0);
            g0 += 2;
            p3 += 2;
            g1++;
            if ((p0 & 0xFFFF) == 10) {
                if (u16(g0) == 13) {
                    put16(g0, 0);
                    r0 = g1;
                    if (r0 == 0) {
                        break;
                    }
                    r0 = 1;
                    break;
                }
            }
        }
        sp += 8;
        return r0;
    }
    
    /**
     * fun_17a14 @ 0x17a14
     * called from: fun_17b94, fun_17d90
     */
    void fun_17a14(int p0) {
        int fp = 0, p1 = 0, r0 = 0;
        sp -= 12;
        put16(sp + 2, p0);
        if ((short) p0 == -1) {
            p0 = 0;
            fp = vStreamOpen(p0, 0x90307);
            put16(sp + 2, p0);
        } else {
            fp = vStreamOpen(g_18cdc, 768) /* "language" */;
        }
        if (fp < 0) {
            vTerminateVMGP();
            throw terminated();
        }
        p0 = fp;
        vStreamWrite(p0, sp + 2, 2);
        vStreamSeek(p0, 2, 0);
        while (fun_17968(fp) != 0) {
            if (u16(g_22e10) != 42) {
                break;
            }
        }
        r0 = s16(sp + 2) * 22;
        while (true) {
            put16(sp + 2, r0);
            if (s16(sp + 2) <= 0) {
                break;
            }
            if (fun_17968(fp) == 0) {
                break;
            }
            r0 = u16(sp + 2) - 1;
        }
        put16(sp + 2, 0);
        p1 = 0;
        while (true) {
            memcpy((s16(sp + 2) << 6) + g_23610, g_22e10, 64);
            put16(sp + 2, p1 + 1);
            fun_17968(fp);
            p1 = u16(sp + 2);
            if ((short) p1 > 20) {
                memcpy(g_23210, g_22e10, 1024);
                vStreamClose(fp);
                fun_17960();
                sp += 12;
                return;
            }
        }
    }
    
    /**
     * fun_17b94 @ 0x17b94
     * called from: fun_11eac, fun_17d90
     */
    int fun_17b94(int p0) {
        int fp = 0, s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, p1 = 0, r0 = 0;
        sp -= 52;
        s5 = p0;
        s3 = 0;
        fp = vStreamOpen(g_18cdc, 256) /* "language" */;
        if (fp >= 0) {
            vStreamSeek(fp, 2, 0);
            s4 = 0;
            while (fun_17968(fp) != 0) {
                if (u16(g_22e10) != 42) {
                    break;
                }
                s4++;
            }
            vStreamSeek(fp, 2, 0);
            s2 = vNewPtr(s4 << 2);
            while (fun_17968(fp) != 0) {
                if (u16(g_22e10) != 42) {
                    break;
                }
                if (s3 >= s4) {
                    break;
                }
                s1 = g_22e12;
                p0 = (fun_1780c(s1) & 0xFFFF) << 1;
                r0 = vNewPtr(p0);
                put32((s3 << 2) + s2, r0);
                fun_177cc(r0, s1);
                s3++;
            }
            B_17ce8: {
                vStreamClose(fp);
                fun_117a4(s3);
                put16(sp, u16(g_19104));
                put16(sp + 4, u16(g_22e08));
                put16(sp + 8, u16(g_22e04));
                put16(sp + 12, 0);
                put32(sp + 16, s5);
                fp = (short) fun_17374(s3, 0, s2, u16(g_18d98));
                p1 = 0;
                if (p1 < s3) {
                    while (true) {
                        vDisposePtr(i32((p1 << 2) + s2));
                        p1++;
                        if (p1 >= s3) {
                            break B_17ce8;
                        }
                    }
                }
            }
            vDisposePtr(s2);
            r0 = 0;
            if (fp >= 0) {
                fun_16f28();
                fun_17a14(fp);
                r0 = 1;
            }
            sp += 52;
            return r0;
        }
        vTerminateVMGP();
        throw terminated();
    }
    
    /**
     * fun_17d90 @ 0x17d90
     * called from: fun_12dd4
     */
    void fun_17d90(int p0) {
        int p1 = 0, p3 = 0;
        sp -= 8;
        p3 = p0;
        p1 = 256;
        p0 = vStreamOpen(g_18cdc, p1) /* "language" */;
        if (p0 < 0) {
            put8(g_18ce5, p1);
            fun_17a14(-1);
        } else {
            put8(g_18ce5, 1);
            vStreamRead(p0, sp + 2, 2);
            vStreamClose(p0);
            p0 = u16(sp + 2);
            if ((p0 & 0xFFFF) == 0xFEFF) {
                if (fun_17b94(p3) == 0) {
                    vTerminateVMGP();
                    throw terminated();
                }
            } else {
                fun_17a14(p0);
            }
        }
        sp += 8;
    }
    
    // ==================================================================== SDK imports
    /** value vCharExtent(ushort charValue) */
    private int vCharExtent(int charValue) { return sys("vCharExtent", charValue, 0, 0, 0); }
    /** void vClearScreen(int color) */
    private void vClearScreen(int color) { sys("vClearScreen", color, 0, 0, 0); }
    /** value vDecompress(VMPtr<byte> source, VMPtr<byte> dest, int streamHandle, uint readBufSize) */
    private int vDecompress(int source, int dest, int streamHandle, int readBufSize) { return sys("vDecompress", source, dest, streamHandle, readBufSize); }
    /** void vDisposePtr(VMPtr<Any> ptr) */
    private void vDisposePtr(int ptr) { sys("vDisposePtr", ptr, 0, 0, 0); }
    /** void vDrawObject(short x, short y, VMPtr<NativeSprite> sprite) */
    private void vDrawObject(int x, int y, int sprite) { sys("vDrawObject", x, y, sprite, 0); }
    /** void vFillRect(short x0, short y0, short x1, short y1) */
    private void vFillRect(int x0, int y0, int x1, int y1) { sys("vFillRect", x0, y0, x1, y1); }
    /** void vFlipScreen(int block) */
    private void vFlipScreen(int block) { sys("vFlipScreen", block, 0, 0, 0); }
    /** value vGetButtonData() */
    private int vGetButtonData() { return sys("vGetButtonData", 0, 0, 0, 0); }
    /** value vGetCaps(CapsQueryType queryType, VMPtr<Any> buffer) */
    private int vGetCaps(int queryType, int buffer) { return sys("vGetCaps", queryType, buffer, 0, 0); }
    /** value vGetRandom() */
    private int vGetRandom() { return sys("vGetRandom", 0, 0, 0, 0); }
    /** value vGetTickCount() */
    private int vGetTickCount() { return sys("vGetTickCount", 0, 0, 0, 0); }
    /** void vGetTimeDate(VMPtr<VMDateTime> dateTime) */
    private void vGetTimeDate(int dateTime) { sys("vGetTimeDate", dateTime, 0, 0, 0); }
    /** value vMapGetAttribute(byte x, byte y) */
    private int vMapGetAttribute(int x, int y) { return sys("vMapGetAttribute", x, y, 0, 0); }
    /** value vMapInit(VMPtr<NativeMapHeader> header) */
    private int vMapInit(int header) { return sys("vMapInit", header, 0, 0, 0); }
    /** void vMapSetXY(short x, short y) */
    private void vMapSetXY(int x, int y) { sys("vMapSetXY", x, y, 0, 0); }
    /** value vMsgBoxU(uint flags, VMWString message) */
    private int vMsgBoxU(int flags, int message) { return sys("vMsgBoxU", flags, message, 0, 0); }
    /** value vNewPtr(uint size) */
    private int vNewPtr(int size) { return sys("vNewPtr", size, 0, 0, 0); }
    /** value vPlayResource(VMPtr<byte> data, uint length, uint flags) */
    private int vPlayResource(int data, int length, int flags) { return sys("vPlayResource", data, length, flags, 0); }
    /** void vPlot(short x, short y) */
    private void vPlot(int x, int y) { sys("vPlot", x, y, 0, 0); }
    /** void vPrint(int transferMode, int x, int y, VMString value) */
    private void vPrint(int transferMode, int x, int y, int value) { sys("vPrint", transferMode, x, y, value); }
    /** value vSelectFont(uint size, uint flags, ushort charShouldAvailable) */
    private int vSelectFont(int size, int flags, int charShouldAvailable) { return sys("vSelectFont", size, flags, charShouldAvailable, 0); }
    /** value vSetActiveFont(VMPtr<VMGPFont> newFont) */
    private int vSetActiveFont(int newFont) { return sys("vSetActiveFont", newFont, 0, 0, 0); }
    /** void vSetBackColor(int color) */
    private void vSetBackColor(int color) { sys("vSetBackColor", color, 0, 0, 0); }
    /** void vSetClipWindow(short x0, short y0, short x1, short y1) */
    private void vSetClipWindow(int x0, int y0, int x1, int y1) { sys("vSetClipWindow", x0, y0, x1, y1); }
    /** void vSetForeColor(int color) */
    private void vSetForeColor(int color) { sys("vSetForeColor", color, 0, 0, 0); }
    /** void vSetPaletteEntry(byte index, uint rgb555Color) */
    private void vSetPaletteEntry(int index, int rgb555Color) { sys("vSetPaletteEntry", index, rgb555Color, 0, 0); }
    /** void vSetRandom(uint seed) */
    private void vSetRandom(int seed) { sys("vSetRandom", seed, 0, 0, 0); }
    /** value vSetTransferMode(uint transferMode) */
    private int vSetTransferMode(int transferMode) { return sys("vSetTransferMode", transferMode, 0, 0, 0); }
    /** value vSpriteBoxCollision(VMPtr<VMGPRect> boxPtr, byte from, byte to) */
    private int vSpriteBoxCollision(int boxPtr, int from, int to) { return sys("vSpriteBoxCollision", boxPtr, from, to, 0); }
    /** void vSpriteClear() */
    private void vSpriteClear() { sys("vSpriteClear", 0, 0, 0, 0); }
    /** value vSpriteCollision(byte slotCheck, byte from, byte to) */
    private int vSpriteCollision(int slotCheck, int from, int to) { return sys("vSpriteCollision", slotCheck, from, to, 0); }
    /** value vSpriteInit(byte count) */
    private int vSpriteInit(int count) { return sys("vSpriteInit", count, 0, 0, 0); }
    /** void vSpriteSet(byte slot, VMPtr<NativeSprite> sprite, short x, short y) */
    private void vSpriteSet(int slot, int sprite, int x, int y) { sys("vSpriteSet", slot, sprite, x, y); }
    /** value vStrCpy(VMPtr<byte> dest, VMPtr<byte> source) */
    private int vStrCpy(int dest, int source) { return sys("vStrCpy", dest, source, 0, 0); }
    /** value vStrLen(VMString str) */
    private int vStrLen(int str) { return sys("vStrLen", str, 0, 0, 0); }
    /** void vStreamClose(int handle) */
    private void vStreamClose(int handle) { sys("vStreamClose", handle, 0, 0, 0); }
    /** value vStreamOpen(VMString fileName, uint mode) */
    private int vStreamOpen(int fileName, int mode) { return sys("vStreamOpen", fileName, mode, 0, 0); }
    /** value vStreamRead(int handle, VMPtr<byte> buffer, int count) */
    private int vStreamRead(int handle, int buffer, int count) { return sys("vStreamRead", handle, buffer, count, 0); }
    /** value vStreamSeek(int handle, int where, StreamSeekMode whence) */
    private int vStreamSeek(int handle, int where, int whence) { return sys("vStreamSeek", handle, where, whence, 0); }
    /** value vStreamWrite(int handle, VMPtr<byte> buffer, int count) */
    private int vStreamWrite(int handle, int buffer, int count) { return sys("vStreamWrite", handle, buffer, count, 0); }
    /** value vSysCtl(int cmd, int op) */
    private int vSysCtl(int cmd, int op) { return sys("vSysCtl", cmd, op, 0, 0); }
    /** void vTerminateVMGP() */
    private void vTerminateVMGP() { sys("vTerminateVMGP", 0, 0, 0, 0); }
    /** value vTextExtent(VMString asciiString) */
    private int vTextExtent(int asciiString) { return sys("vTextExtent", asciiString, 0, 0, 0); }
    /** value vTextExtentU(VMString utf16String) */
    private int vTextExtentU(int utf16String) { return sys("vTextExtentU", utf16String, 0, 0, 0); }
    /** void vTextOut(short x, short y, VMString text) */
    private void vTextOut(int x, int y, int text) { sys("vTextOut", x, y, text, 0); }
    /** void vTextOutU(short x, short y, VMString text) */
    private void vTextOutU(int x, int y, int text) { sys("vTextOutU", x, y, text, 0); }
    /** void vUpdateMap() */
    private void vUpdateMap() { sys("vUpdateMap", 0, 0, 0, 0); }
    /** void vUpdateSprite() */
    private void vUpdateSprite() { sys("vUpdateSprite", 0, 0, 0, 0); }
    /** value vitoa(int val, VMPtr<byte> buf, byte len, byte pad) */
    private int vitoa(int val, int buf, int len, int pad) { return sys("vitoa", val, buf, len, pad); }
    /** value vutoa(uint val, VMPtr<byte> buf, byte len, byte pad) */
    private int vutoa(int val, int buf, int len, int pad) { return sys("vutoa", val, buf, len, pad); }
}
