package absence.plugin;

import absence.plugin.AbsencePlugin.AbsenceTarget.*;
import arc.struct.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.source.util.TaskEvent.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;

import java.lang.annotation.*;

public class AbsencePlugin implements Plugin{
    Seq<JCMethodInvocation> classes = new Seq<>(), packages = new Seq<>();
    Seq<String> classDefs = new Seq<>(), packDefs = new Seq<>();

    @Override
    public void init(JavacTask task, String... args){
        TreeMaker maker = TreeMaker.instance(((JavacTaskImpl)task).getContext());
        task.addTaskListener(new TaskListener(){
            @Override
            public void finished(TaskEvent e){
                if(e.getKind() == Kind.PARSE){
                    e.getCompilationUnit().accept(new TreeScanner<Void, Void>(){
                        @Override
                        public Void visitVariable(VariableTree node, Void unused){
                            AbsenceType type = valid(node);
                            if(type == AbsenceType.classType){
                                classes.add((JCMethodInvocation)node.getInitializer());
                            }else if(type == AbsenceType.packageType){
                                packages.add((JCMethodInvocation)node.getInitializer());
                            }

                            return super.visitVariable(node, unused);
                        }
                    }, null);
                }else if(e.getKind() == Kind.ENTER){
                    e.getCompilationUnit().accept(new TreeScanner<Void, Void>(){
                        @Override
                        public Void visitClass(ClassTree node, Void unused){
                            ClassSymbol sym = ((JCClassDecl)node).sym;
                            classDefs.add(sym.className());

                            Symbol current = sym;
                            while(!(current instanceof PackageSymbol) && !(current.asType() instanceof JCNoType)){
                                current = current.getEnclosingElement();
                            }

                            packDefs.add(current.getQualifiedName().toString());
                            return super.visitClass(node, unused);
                        }
                    }, null);
                }
            }

            @Override
            public void started(TaskEvent event){
                if(event.getKind() != Kind.ANALYZE) return;

                classDefs.distinct().sort();
                packDefs.distinct().sort();

                for(JCMethodInvocation inv : classes){
                    inv.args = List.from(classDefs.map(maker::Literal));
                }

                for(JCMethodInvocation inv : packages){
                    inv.args = List.from(packDefs.map(maker::Literal));
                }
            }
        });
    }

    JCAnnotation type(VariableTree node){
        for(AnnotationTree ann : node.getModifiers().getAnnotations()){
            if(ann.getAnnotationType().toString().equals(AbsenceTarget.class.getSimpleName())){
                return (JCAnnotation)ann;
            }
        }

        return null;
    }

    AbsenceType value(JCAnnotation ann){
        for(JCExpression param : ann.getArguments()){
            JCExpression value = param;
            if(value instanceof JCAssign){
                JCAssign assign = (JCAssign)value;
                if(assign.lhs.toString().equals("value")){
                    value = assign.rhs;
                }
            }

            Name name;
            if(value instanceof JCFieldAccess){
                name = ((JCFieldAccess)value).name;
            }else if(value instanceof JCIdent){
                name = ((JCIdent)value).name;
            }else{
                throw new IllegalArgumentException(value.getClass().getSimpleName());
            }

            return AbsenceType.valueOf(name.toString());
        }

        return null;
    }

    AbsenceType valid(VariableTree node){
        JCAnnotation ann = type(node);
        AbsenceType type;

        if(
            ann != null && (type = value(ann)) != null &&
            node.getType().toString().equals("Seq<String>") &&
            (node.getInitializer() != null && node.getInitializer().toString().startsWith("Seq.with("))
        ){
            return type;
        }else{
            return null;
        }
    }

    @Override
    public boolean autoStart(){
        return true;
    }

    @Override
    public String getName(){
        return "absence";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface AbsenceTarget{
        AbsenceType value();

        enum AbsenceType{
            classType,
            packageType
        }
    }
}
