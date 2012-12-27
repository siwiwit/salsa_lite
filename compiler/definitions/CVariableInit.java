package salsa_lite.compiler.definitions;

import salsa_lite.compiler.symbol_table.SymbolTable;
import salsa_lite.compiler.symbol_table.SalsaNotFoundException;
import salsa_lite.compiler.symbol_table.TypeSymbol;
import salsa_lite.compiler.symbol_table.VariableDeclarationException;

public class CVariableInit extends CErrorInformation {
	public String name;
	public CExpression expression;

	public boolean isToken() {
		if (expression != null && expression.isToken()) return true;

        try {
    		if (name != null && SymbolTable.isToken(name)) return true;
        } catch (SalsaNotFoundException snfe) {
            CompilerErrors.printErrorMessage("[CVariableInit.isToken]: " + snfe.toString(), this);
            throw new RuntimeException(snfe);
        }
		return false;
	}

	public String toJavaCodeAsToken(String type, boolean is_token_director, boolean is_continuation_director) {
//        System.err.println("toJavaCodeAsToken for: '" + name + "'");
		String code = name;

        TypeSymbol ts = null;
        try {
            ts = SymbolTable.getTypeSymbol(type);

            if (expression != null) {
                if (expression.getType().canMatch(ts) < 0) {
                    CompilerErrors.printErrorMessage("Conflicting types.  Cannot assign '" + expression.getType().getLongSignature() + "' to '" + ts.getLongSignature() + "'", expression);
                }

                if (!(is_token_director || is_continuation_director) && expression.isToken()) {
                    CompilerErrors.printErrorMessage("Cannot assign a token to a non-token: '" + expression.getType().getLongSignature() + "'.", expression);
                }

                boolean previous_continues = false, previous_token = false;
                if (is_continuation_director) {
                    previous_continues = SymbolTable.continuationTokenMessage;
                    SymbolTable.continuationTokenMessage = true;
                } else if (is_token_director) {
                    previous_token = SymbolTable.isExpressionContinuation;
                    SymbolTable.isExpressionContinuation = true;
                }

                if (expression.isToken()) {
//                    System.err.println("setting '" + name + "' equal to token expression");
                    code += " = " + expression.toJavaCode();
                } else if (is_continuation_director) {
//                    System.err.println("setting '" + name + "' equal to continuation director");
                    code += " = ContinuationDirector.construct(1, new Object[]{" + expression.toJavaCode() + "})";
                } else {
//                    System.err.println("setting '" + name + "' equal to token director");
                    code += " = TokenDirector.construct(1, new Object[]{" + expression.toJavaCode() + "})";
                }

                SymbolTable.continuationTokenDirectorName = name;

                if (is_continuation_director) {
                    SymbolTable.continuationTokenMessage = previous_continues;
                } else if (is_token_director) {
                    SymbolTable.isExpressionContinuation = previous_token;
                }
            }
        } catch (VariableDeclarationException vde) {
            CompilerErrors.printErrorMessage("Could not declare type '" + type + "' for variable initialization. " + vde.toString(), this);
            throw new RuntimeException(vde);
        } catch (SalsaNotFoundException snfe) {
            CompilerErrors.printErrorMessage("Could not determine type '" + type + "' for variable initialization.", this);
            throw new RuntimeException(snfe);
        }

		return code;
	}


	public String toJavaCode(String type, boolean is_token) {
		String code = name;

        TypeSymbol ts = null;
        try {
            ts = SymbolTable.getTypeSymbol(type);

            if (expression != null) {
                if (expression.getType().canMatch(ts) < 0) {
                    CompilerErrors.printErrorMessage("Conflicting types.  Cannot assign '" + expression.getType().getLongSignature() + "' to '" + ts.getLongSignature() + "'", expression);
                }

                if (is_token && expression.isToken()) {
                    CompilerErrors.printErrorMessage("Cannot assign a token to a non-token.", expression);
                }

                code += " = " + expression.toJavaCode();
            }
        } catch (VariableDeclarationException vde) {
            CompilerErrors.printErrorMessage("Could not declare '" + type + "' for variable initialization. " + vde.toString(), this);
            throw new RuntimeException(vde);
        } catch (SalsaNotFoundException snfe) {
            CompilerErrors.printErrorMessage("Could not determine type '" + type + "' for variable initialization.", this);
            throw new RuntimeException(snfe);
        }

		return code;
	}

	public String toSalsaCode() {
		return "";
	}
}
