/**
 * Copyright (C) 2006-2019 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) of the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.reflect.factory;
import java.util.Arrays;
import java.util.function.Function;
import spoon.reflect.code.CtExpression;
import spoon.reflect.reference.CtTypeReference;
public class CodeFactory {
    public void createNewClass(CtExpression<?>... parameters) {
        Arrays.stream(parameters).map(( x) -> x.getType());
        Arrays.toString(parameters);
    }
}