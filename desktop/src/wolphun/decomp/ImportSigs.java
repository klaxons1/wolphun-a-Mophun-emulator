package wolphun.decomp;

import java.util.HashMap;

/**
 * Prototypes of the mophun SDK functions a VMGP game can import, so the decompiler can print
 * {@code vFillRect(p0, p1, p2, p3)} with the right number of arguments and knows whether the
 * call produces a value in r0.
 *
 * The table was generated from the [ModuleCall] declarations of nofun
 * (github.com/RadratSoftworks/nofun, Assets/Scripts/Module) -- names, argument names, argument
 * types and void/non-void return. Imports that are not listed are treated as "4 register
 * arguments, returns a value", which is always safe (PIP2 passes p0..p3 in registers; extra
 * arguments live on the stack, which the decompiled code keeps in emulated memory anyway).
 */
public final class ImportSigs {
    static final class Sig {
        final String name; final boolean returnsValue; final String[] types; final String[] names;
        Sig(String name, boolean returnsValue, String[] types, String[] names){
            this.name=name; this.returnsValue=returnsValue; this.types=types; this.names=names;
        }
        /** arguments passed in p0..p3 (the rest, if any, are on the stack) */
        int regArgs(){ return Math.min(4, names.length); }
    }
    private static final HashMap<String,Sig> SIGS = new HashMap<String,Sig>();

    static Sig get(String name){
        Sig s = SIGS.get(name);
        if(s!=null) return s;
        return new Sig(name, true, new String[]{"int","int","int","int"}, new String[]{"a0","a1","a2","a3"});
    }
    /** number of register arguments of an SDK function (4 if unknown) */
    public static int regArgCount(String name){ Sig g=SIGS.get(name); return g==null? 4 : g.regArgs(); }
    /** bits of argument k the SDK function looks at (byte = 8, short/ushort = 16, else 32) */
    public static int argBits(String name, int k){
        Sig g=SIGS.get(name);
        if(g==null || k>=g.types.length) return 32;
        String t=g.types[k];
        return t.equals("byte")? 8 : (t.equals("short")||t.equals("ushort"))? 16 : 32;
    }
    /** an argument as the SDK function sees it: byte -> unsigned 8 bits, short -> signed 16, ushort -> unsigned 16 */
    public static int normArg(String name, int k, int v){
        Sig g=SIGS.get(name);
        if(g==null || k>=g.types.length) return v;
        String t=g.types[k];
        return t.equals("byte")? v&0xFF : t.equals("short")? (short)v : t.equals("ushort")? v&0xFFFF : v;
    }
    /** does the SDK function return a value (false: r0 is left as it was) */
    public static boolean returns(String name){ Sig g=SIGS.get(name); return g==null || g.returnsValue; }
    static boolean known(String name){ return SIGS.containsKey(name); }

    private static void s(String name, boolean ret, String params){
        String[] types, names;
        if(params.isEmpty()){ types=new String[0]; names=new String[0]; }
        else {
            String[] ps = params.split(",\\s*");
            types=new String[ps.length]; names=new String[ps.length];
            for(int i=0;i<ps.length;i++){
                int sp = ps[i].lastIndexOf(' ');
                types[i]=ps[i].substring(0,sp).trim(); names[i]=ps[i].substring(sp+1).trim();
            }
        }
        SIGS.put(name, new Sig(name, ret, types, names));
    }

    static {
        s("vBoxInViewFrustum", true, "VMPtr<NativeBBox> boxPtr");
        s("vCharExtent", true, "ushort charValue");
        s("vCheckDataCert", true, "VMPtr<byte> cert");
        s("vCheckDataCertFile", true, "int handle");
        s("vClearScreen", false, "int color");
        s("vCollisionBoxBox", true, "VMPtr<NativeBBox> box1, VMPtr<NativeBBox> box2");
        s("vCollisionPointBox", true, "VMPtr<NativeVector3D> point, VMPtr<NativeBBox> box");
        s("vCollisionVectorPlane", true, "VMPtr<NativeVector3D> collisionPointPtr, VMPtr<NativeVector3D> linePointsPtr, VMPtr<NativePlane> planePtr");
        s("vCollisionVectorPoly", true, "VMPtr<NativeVector3D> collisionPosPtr, VMPtr<NativeVector3D> lineVPtr, VMPtr<NativeVector3D> polygonVPtr, VMPtr<NativePlane> planePtr");
        s("vCopyRect", false, "");
        s("vCos", true, "int fixedV");
        s("vCreatePlaneFromPoly", false, "VMPtr<NativePlane> destPlanePtr, VMPtr<NativeVector3D> polygonsPtr");
        s("vCreateTask", true, "uint addr, uint p0, uint p1, uint p2");
        s("vCreateTexture", true, "VMPtr<NativeTexture> tex");
        s("vCrossProduct", false, "VMPtr<NativeVector3D> dest, VMPtr<NativeVector3D> lhs, VMPtr<NativeVector3D> rhs");
        s("vDecompHdr", true, "VMPtr<NativeCompressedFileInfo> infoPtr, VMPtr<byte> source");
        s("vDecompress", true, "VMPtr<byte> source, VMPtr<byte> dest, int streamHandle, uint readBufSize");
        s("vDeleteAllTextures", false, "");
        s("vDeleteTexture", false, "int handle");
        s("vDisposePtr", false, "VMPtr<Any> ptr");
        s("vDisposeTask", false, "int handle");
        s("vDiv", true, "int fixedA, int fixedB");
        s("vDotProduct", true, "VMPtr<NativeVector3D> lhs, VMPtr<NativeVector3D> rhs");
        s("vDrawBillboard", false, "VMPtr<NativeBillboard> billboardPtr");
        s("vDrawFlatPolygon", false, "VMPtr<Triangle> triPtr");
        s("vDrawLine", false, "short x0, short y0, short x1, short y1");
        s("vDrawObject", false, "short x, short y, VMPtr<NativeSprite> sprite");
        s("vDrawPolygon", false, "VMPtr<NativeVertexGST> v1Ptr, VMPtr<NativeVertexGST> v2Ptr, VMPtr<NativeVertexGST> v3Ptr");
        s("vFillRect", false, "short x0, short y0, short x1, short y1");
        s("vFindRGBIndex", true, "uint rgb");
        s("vFlipScreen", false, "int block");
        s("vFrameTickCount", true, "");
        s("vFreeTexture", false, "int handle");
        s("vGetButtonData", true, "");
        s("vGetCaps", true, "CapsQueryType queryType, VMPtr<Any> buffer");
        s("vGetInteger", true, "uint key, VMPtr<uint> value");
        s("vGetPaletteEntry", true, "byte index");
        s("vGetPixel", true, "short x, short y");
        s("vGetRandom", true, "");
        s("vGetTickCount", true, "");
        s("vGetTimeDate", false, "VMPtr<VMDateTime> dateTime");
        s("vGetTimeDateUTC", false, "VMPtr<VMDateTime> dateTime");
        s("vGetVMGPInfo", true, "");
        s("vInit3D", false, "");
        s("vMapDispose", false, "");
        s("vMapGetAttribute", true, "byte x, byte y");
        s("vMapGetTile", true, "byte x, byte y");
        s("vMapInit", true, "VMPtr<NativeMapHeader> header");
        s("vMapSetAttribute", false, "byte x, byte y, byte attribute");
        s("vMapSetTile", false, "byte x, byte y, byte tile");
        s("vMapSetXY", false, "short x, short y");
        s("vMatrixGetCurrent", false, "VMPtr<V3DMatrix> matrixPtr");
        s("vMatrixIdentity", false, "");
        s("vMatrixInvert", false, "");
        s("vMatrixLookAt", false, "VMPtr<NativeVector3D> vEyePtr, VMPtr<NativeVector3D> vAtptr, VMPtr<NativeVector3D> vUpPtr");
        s("vMatrixMultiply", false, "VMPtr<V3DMatrix> matrixPtr");
        s("vMatrixMultiply3x3", false, "VMPtr<V3DMatrix> matrixPtr");
        s("vMatrixOrtho", false, "int widthFixed, int heightFixed, int zNearFixed, int zFarFixed");
        s("vMatrixPerspective", false, "int widthFixed, int heightFixed, int zNearFixed, int zFarFixed");
        s("vMatrixRotateVector", false, "VMPtr<NativeVector3D> axisPtr, short d");
        s("vMatrixRotateX", false, "short d");
        s("vMatrixRotateY", false, "short d");
        s("vMatrixRotateZ", false, "short d");
        s("vMatrixScale", false, "int xFixed, int yFixed, int zFixed");
        s("vMatrixSetCurrent", false, "VMPtr<V3DMatrix> matrixPtr");
        s("vMatrixSetLight", false, "VMPtr<V3DMatrix> matrixPtr");
        s("vMatrixTranslate", false, "int xFixed, int yFixed, int zFixed");
        s("vMemFree", true, "");
        s("vMsgBox", true, "uint flags, VMString message, VMString optionalTitle");
        s("vMul", true, "int fixedA, int fixedB");
        s("vMusicCtrl", true, "int handle, NativeMusicControlCode control");
        s("vMusicCtrlEx", true, "int handle, NativeMusicControlCode control, int parameters");
        s("vMusicDisposeHandle", true, "int handle");
        s("vMusicGetHandle", true, "VMPtr<byte> musicData");
        s("vMusicLoad", true, "int handle, int type");
        s("vNewPtr", true, "uint size");
        s("vPlayResource", true, "VMPtr<byte> data, uint length, uint flags");
        s("vPlot", false, "short x, short y");
        s("vPrint", false, "int transferMode, int x, int y, VMString value");
        s("vReceive", true, "");
        s("vReceiveAny", true, "int handle");
        s("vRenderPrimitive", true, "VMPtr<NativeMesh> mesh, uint topology");
        s("vRenderPrimitiveIndexed", true, "VMPtr<short> indexList, short indexCount, VMPtr<NativeMesh> mesh, uint topology");
        s("vResetLights", false, "");
        s("vScanKeys", true, "");
        s("vSelectFont", true, "uint size, uint flags, ushort charShouldAvailable");
        s("vSend", false, "int handle, int data");
        s("vSetActiveFont", true, "VMPtr<VMGPFont> newFont");
        s("vSetActiveTexture", false, "int index");
        s("vSetAmbientLight", false, "uint colour");
        s("vSetBackColor", false, "int color");
        s("vSetClipWindow", false, "short x0, short y0, short x1, short y1");
        s("vSetDisplayWindow", false, "int width, int height");
        s("vSetFogColor", false, "uint colour");
        s("vSetForeColor", false, "int color");
        s("vSetLight", false, "int index, VMPtr<NativeLight> lightPtr");
        s("vSetMaterial", false, "VMPtr<NativeMaterial> materialPtr");
        s("vSetMaterial2", false, "VMPtr<NativeMaterial2> materialPtr");
        s("vSetMatrixMode", false, "int mode");
        s("vSetPalette", false, "VMPtr<ushort> colors, byte startIndex, ushort count");
        s("vSetPaletteEntry", false, "byte index, uint rgb555Color");
        s("vSetRandom", false, "uint seed");
        s("vSetRenderState", false, "RenderState state, uint value");
        s("vSetStackSize", true, "uint size");
        s("vSetTexture", true, "VMPtr<byte> textureData, uint format, uint lods, uint mipCount");
        s("vSetTransferMode", true, "uint transferMode");
        s("vSetViewport", false, "int left, int top, int width, int height");
        s("vSetZBuffer", false, "ushort value");
        s("vSin", true, "int fixedV");
        s("vSoundCtrl", true, "int handle, NativeSoundControlCode control");
        s("vSoundCtrlEx", true, "int handle, NativeSoundControlCode control, int parameters");
        s("vSoundDispose", true, "");
        s("vSoundDisposeHandle", true, "int handle");
        s("vSoundGetHandle", true, "VMPtr<byte> soundData");
        s("vSprintf", false, "VMPtr<byte> buf, VMString message");
        s("vSpriteBoxCollision", true, "VMPtr<VMGPRect> boxPtr, byte from, byte to");
        s("vSpriteClear", false, "");
        s("vSpriteCollision", true, "byte slotCheck, byte from, byte to");
        s("vSpriteDispose", false, "");
        s("vSpriteInit", true, "byte count");
        s("vSpriteSet", false, "byte slot, VMPtr<NativeSprite> sprite, short x, short y");
        s("vSqrt", true, "int fixedV");
        s("vStrCpy", true, "VMPtr<byte> dest, VMPtr<byte> source");
        s("vStrLen", true, "VMString str");
        s("vStreamClose", false, "int handle");
        s("vStreamOpen", true, "VMString fileName, uint mode");
        s("vStreamRead", true, "int handle, VMPtr<byte> buffer, int count");
        s("vStreamSeek", true, "int handle, int where, StreamSeekMode whence");
        s("vStreamWrite", true, "int handle, VMPtr<byte> buffer, int count");
        s("vSysCtl", true, "int cmd, int op");
        s("vTan", true, "int fixedV");
        s("vTaskAlive", true, "int handle");
        s("vTerminateVMGP", false, "");
        s("vTestKey", true, "uint key");
        s("vTextExtent", true, "VMString asciiString");
        s("vTextExtentU", true, "VMString utf16String");
        s("vTextOut", false, "short x, short y, VMString text");
        s("vTextOutU", false, "short x, short y, VMString text");
        s("vThisTask", true, "");
        s("vUID", true, "");
        s("vUpdateMap", false, "");
        s("vUpdateSprite", false, "");
        s("vUpdateSpriteMap", false, "");
        s("vVectorAdd", false, "VMPtr<NativeVector3D> dest, VMPtr<NativeVector3D> lhs, VMPtr<NativeVector3D> rhs");
        s("vVectorArrayAdd", false, "VMPtr<NativeVector3D> destPtr, VMPtr<NativeVector3D> source1Ptr, VMPtr<NativeVector3D> source2Ptr, int count");
        s("vVectorArrayDelta", false, "VMPtr<NativeVector3D> destPtr, VMPtr<NativeVector3D> source1Ptr, VMPtr<NativeVector3D> source2Ptr, uint param");
        s("vVectorMul", false, "VMPtr<NativeVector3D> dest, VMPtr<NativeVector3D> lhs, VMPtr<NativeVector3D> rhs");
        s("vVectorNormalize", false, "VMPtr<NativeVector3D> dest");
        s("vVectorProjectV3", false, "VMPtr<NativeVector4D> destPtr, VMPtr<NativeVector3D> sourcePtr, int count");
        s("vVectorProjectV4", false, "VMPtr<NativeVector4D> destPtr, VMPtr<NativeVector4D> sourcePtr, int count");
        s("vVectorSub", false, "VMPtr<NativeVector3D> dest, VMPtr<NativeVector3D> lhs, VMPtr<NativeVector3D> rhs");
        s("vVectorTransformV3", false, "VMPtr<NativeVector3D> destination, VMPtr<NativeVector3D> source, int count");
        s("vVectorTransformV4", false, "VMPtr<NativeVector4D> destination, VMPtr<NativeVector3D> source, int count");
        s("vWaitVBL", true, "int block");
        s("vatoi", true, "VMPtr<byte> str, VMPtr<uint> end");
        s("vitoa", true, "int val, VMPtr<byte> buf, byte len, byte pad");
        s("vutoa", true, "uint val, VMPtr<byte> buf, byte len, byte pad");
        s("vMsgBoxU", true, "uint flags, VMWString message");   // title: on the stack when flags & 0x80 (see Sdk)
        s("vPrintU", false, "int transferMode, int x, int y, VMWString value");
    }
}
