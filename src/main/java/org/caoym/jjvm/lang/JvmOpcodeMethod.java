package org.caoym.jjvm;

import com.sun.tools.classfile.*;
import org.caoym.jjvm.runtime.*;

/**
 * 字节码方法（区别于 native 方法）
 */
public class JvmOpcodeMethod implements JvmMethod {

    private ClassFile classFile;
    private Method method;
    private OpcodeInvoker[] opcodes;
    private Code_attribute codeAttribute;

    public JvmOpcodeMethod(ClassFile classFile, Method method) {
        this.classFile = classFile;
        this.method = method;
        codeAttribute = (Code_attribute)method.attributes.get("Code");
        opcodes = BytecodeInterpreter.parseCodes(codeAttribute.code);
    }

    /**
     * 解释执行方法的字节码
     */
    public void call(Env env, Object thiz, Object ...args) throws Exception {
        // 每次方法调用都产生一个新的栈帧，当前方法返回后，将其（栈帧）设置为已返回，BytecodeInterpreter.run会在检查到返回后，将栈帧推
        // 出栈，并将返回值（如果有）推入上一个栈帧的操作数栈

        StackFrame frame = env.getStack().newFrame(
                classFile.constant_pool,
                opcodes,
                codeAttribute.max_locals,
                codeAttribute.max_stack);

        // Java 虚拟机使用局部变量表来完成方法调用时的参数传递，当一个方法被调用的时候，它的 参数将会传递至从 0 开始的连续的局部变量表位置
        // 上。特别地，当一个实例方法被调用的时候， 第 0 个局部变量一定是用来存储被调用的实例方法所在的对象的引用(即 Java 语言中的“this”
        // 关键字)。后续的其他参数将会传递至从 1 开始的连续的局部变量表位置上。

        Slots<Object> locals = frame.getLocalVariables();
        int pos = 0;
        if(!method.access_flags.is(AccessFlags.ACC_STATIC)){
            locals.set(0, thiz, 1);
            pos++;
        }

        for (Object arg : args) {
            locals.set(pos++, arg, 1);
        }

        BytecodeInterpreter.run(env);
    }

    public AccessFlags getAccessFlags() {
        return method.access_flags;
    }
}