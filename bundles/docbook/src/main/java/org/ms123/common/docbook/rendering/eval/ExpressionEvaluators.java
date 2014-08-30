/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.docbook.rendering.eval;

public enum ExpressionEvaluators {

    MVEL(MvelExpressionEvaluator.class), ;

    private Class<? extends ExpressionEvaluator> evaluatorClass;

    private ExpressionEvaluator evaluator;

    private ExpressionEvaluators(
            Class<? extends ExpressionEvaluator> evaluatorClass) {
        this.evaluatorClass = evaluatorClass;
    }

    public ExpressionEvaluator getEvaluator() {

        if (evaluator == null) {
            try {
                evaluator = evaluatorClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error creating expression evaluator!", e);
            }
        }

        return this.evaluator;
    }

    public static final ExpressionEvaluators lookup(String type) {
        for (ExpressionEvaluators e : ExpressionEvaluators.values()) {
            if (e.name().equalsIgnoreCase(type))
                return e;
        }

        return null;
    }

}
