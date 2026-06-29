package com.apkpatcher.dex;

import java.util.List;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction22c;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference;

public class Patch {

    public static ImmutableMethod patch(
        Method method,
        MethodImplementation implementation) {

        return new ImmutableMethod(
            method.getDefiningClass(),
            method.getName(),
            method.getParameters(),
            method.getReturnType(),
            method.getAccessFlags(),
            method.getAnnotations(),
            method.getHiddenApiRestrictions(),
            implementation
        );
    }

    private static int originalRegs(Method method) {
        return method.getImplementation().getRegisterCount();
    }

    public static ImmutableMethod returnNull(Method method) {
        return patch(
            method, new ImmutableMethodImplementation(
                originalRegs(method),
                List.of(
                    // const/4 v0, 0x0
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                    // move-result-object v0
                    new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                ),
                null, null
            )
        );
    }

    public static ImmutableMethod returnVoid(Method method) {
        return patch(
            method, new ImmutableMethodImplementation(
                originalRegs(method),
                List.of(
                    // return-void
                    new ImmutableInstruction10x(Opcode.RETURN_VOID)
                ),
                null, null
            )
        );
    }

    public static ImmutableMethod returnValue(Method method, boolean value) {
        return patch(method, new ImmutableMethodImplementation(
                originalRegs(method),
                List.of(
                    // const/4 p0, 0x1 || const/4 p0, 0x0
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, value ? 1 : 0),
                    // return v0
                    new ImmutableInstruction11x(Opcode.RETURN, 0)
                ),
                null, null
            )
        );
    }

    public static ImmutableMethod returnEmptyArray(Method method) {
        return patch(
            method, new ImmutableMethodImplementation(
                originalRegs(method),
                List.of(
                    // const/4 v0, 0x0
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                    // new-array v0, v0, [Ljava/security/cert/X509Certificate;
                    new ImmutableInstruction22c(
                        Opcode.NEW_ARRAY, 0, 0,
                        new ImmutableTypeReference(
                            "[Ljava/security/cert/X509Certificate;"
                        )
                    ),
                    // return-object v0
                    new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                ),
                null, null
            )
        );
    }
}