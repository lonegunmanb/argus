package org.apache.uss.argus.function

object Functions {
    private val functions: MutableMap<String, Function> = HashMap()

    init {
        functions["concat"] = Concat.instance
        // functions["concat_ws"] = Concat.instance
        //functions["bin"] = Bin.instance
//        functions["bit_length"] = BitLength.instance
//        functions["insert"] = Insert.instance
//        functions["instr"] = Instr.instance
//        functions["char"] = com.alibaba.druid.sql.visitor.functions.Char.instance
        // functions["elt"] = Elt.instance
//        functions["left"] = Left.instance
//        functions["locate"] = Locate.instance
//        functions["lpad"] = Lpad.instance
//        functions["ltrim"] = Ltrim.instance
//        functions["mid"] = Substring.instance
//        functions["substr"] = Substring.instance
//        functions["substring"] = Substring.instance
//        functions["right"] = Right.instance
//        functions["reverse"] = Reverse.instance
//        functions["len"] = Length.instance
//        functions["length"] = Length.instance
//        functions["char_length"] = Length.instance
//        functions["character_length"] = Length.instance
//        functions["trim"] = Trim.instance
//        functions["ucase"] = Ucase.instance
//        functions["upper"] = Ucase.instance
//        functions["lcase"] = Lcase.instance
//        functions["lower"] = Lcase.instance
//        functions["hex"] = Hex.instance
//        functions["unhex"] = Unhex.instance
//        functions["greatest"] = Greatest.instance
//        functions["least"] = Least.instance
//        functions["isnull"] = Isnull.instance
//        functions["if"] = If.instance
//
//        functions["md5"] = OneParamFunctions.instance
//        functions["bit_count"] = OneParamFunctions.instance
//        functions["soundex"] = OneParamFunctions.instance
//        functions["space"] = OneParamFunctions.instance


        //functions["now"] = Now.instance
        //functions["ascii"] = Ascii.instance
    }

    fun get(name: String): Function? {
        return functions[name]
    }
}