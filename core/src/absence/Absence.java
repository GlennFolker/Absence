package absence;

import absence.plugin.AbsencePlugin.*;
import absence.plugin.AbsencePlugin.AbsenceTarget.*;
import arc.struct.*;
import arc.util.*;

@SuppressWarnings("unused")
public class Absence{
    @AbsenceTarget(AbsenceType.classType)
    public static final Seq<String> classes = Seq.with();

    @AbsenceTarget(AbsenceType.packageType)
    public static final Seq<String> packages = Seq.with();

    public static void main(String[] args){
        Log.info("Classes: [\n\t@\n]", classes.toString("\n\t"));
        Log.info("Packages: [\n\t@\n]", packages.toString("\n\t"));
    }

    public static class Inner{
        public static class InnerInner{}
    }
}
