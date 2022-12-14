package me.blvckbytes.gpeee.interpreter;

import me.blvckbytes.gpeee.functions.FExpressionFunction;

import java.util.Map;
import java.util.function.Supplier;

public interface IEvaluationEnvironment {

  /**
   * Mapping identifiers to available functions which an expression may invoke
   */
  Map<String, FExpressionFunction> getFunctions();

  /**
   * Mapping identifiers to available live variables which an expression may resolve
   */
  Map<String, Supplier<ExpressionValue>> getLiveVariables();

  /**
   * Mapping identifiers to available static variables which an expression may resolve
   */
  Map<String, ExpressionValue> getStaticVariables();

}
