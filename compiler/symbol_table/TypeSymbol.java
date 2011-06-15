package salsa_lite.compiler.symbol_table;

import salsa_lite.compiler.definitions.CExpression;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Vector;

public class TypeSymbol implements Comparable<TypeSymbol> {
    TypeSymbol superType = null;
    public TypeSymbol getSuperType()    { return superType; }

    String name;
    public String getName()             { return name; }

    String module;
    public String getModule()           { return module; }

    public String getSignature()        { return name; }
    public String getLongSignature()    { return module + name; }

    public int compareTo(TypeSymbol other) {
        return getLongSignature().compareTo(other.getLongSignature());
    }

    public boolean equals(TypeSymbol other) {
        return this.getLongSignature().equals( other.getLongSignature() );
    }

	public LinkedHashMap<String,FieldSymbol> fields                = new LinkedHashMap<String,FieldSymbol>();
    public LinkedHashMap<String,ConstructorSymbol> constructors    = new LinkedHashMap<String,ConstructorSymbol>();

	public FieldSymbol          getField(int i)             { return new ArrayList<FieldSymbol>(fields.values()).get(i); }
	public ConstructorSymbol    getConstructor(int i)       { return new ArrayList<ConstructorSymbol>(constructors.values()).get(i); }

    public FieldSymbol          getField(String s)          { return fields.get(s); }
    public ConstructorSymbol    getConstructor(String s)    { return constructors.get(s); }

    public ConstructorSymbol    getConstructor(Vector<CExpression> expressions) throws SalsaNotFoundException {
        return (ConstructorSymbol)Invokable.matchInvokable(new Invokable(this, expressions), constructors);
    }

    public TypeSymbol getFieldType(String fieldName) {
        ArrayList<FieldSymbol> fl = new ArrayList<FieldSymbol>(fields.values());

        for (FieldSymbol f : fl) {
            if (f.getName().equals(fieldName)) return f.getType();
        }

        if (superType != null) {
            return superType.getFieldType(fieldName);
        }
        return null;
    }

    /**
     *  This type matches the target if the target is the same type, or a superclass of this type
     */
    public boolean canMatch(TypeSymbol target) throws SalsaNotFoundException {
        if (this.equals(target)) return true;

        if (this.getName().equals("null")) {
            if (target instanceof PrimitiveType) return false;
            else return true; // null can match any actor, array or object
        }

        if (this instanceof PrimitiveType && !(target instanceof PrimitiveType)) {
            TypeSymbol nonPrimitive = SymbolTable.getTypeSymbol( toNonPrimitiveString() );
            return nonPrimitive.canMatch(target);
        }

        if (superType != null) return superType.canMatch(target);
        return false;
    }

    public String toNonPrimitiveString() {
		if (name.equals("short")) return "Short";
		else if (name.equals("int")) return "Integer";
		else if (name.equals("long")) return "Long";
		else if (name.equals("float")) return "Float";
		else if (name.equals("double")) return "Double";
		else if (name.equals("boolean")) return "Boolean";
		else if (name.equals("char")) return "Character";
		else if (name.equals("byte")) return "Byte";
		else return getSignature();
    }
}