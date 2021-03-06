package salsa_lite.compiler.symbol_table;


public class VariableTypeSymbol {

    boolean isToken;
    boolean isStatic;
    boolean isGeneric;
    String name;
    TypeSymbol type;

    public boolean isToken()    { return isToken; }
    public boolean isGeneric()  { return isGeneric; }
    public boolean isStatic()   { return isStatic; }
    public String getName()     { return name; }
    public TypeSymbol getType() { return type; }

    public VariableTypeSymbol(String name, TypeSymbol type, boolean isToken, boolean isStatic, boolean isGeneric) {
        this.name = name;
        this.type = type;
        this.isToken = isToken;
        this.isStatic = isStatic;
        this.isGeneric = isGeneric;
    }
}
