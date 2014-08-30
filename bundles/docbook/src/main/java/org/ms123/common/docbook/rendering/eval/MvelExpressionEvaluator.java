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

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class MvelExpressionEvaluator implements ExpressionEvaluator {

    private static final Logger log = LoggerFactory
            .getLogger(MvelExpressionEvaluator.class);

    public String evaluate(String expression, Map<String, Object> context) {

        try {

            Object result = MVEL.eval(expression, context);
            if (result != null)
                return String.valueOf(result);

            return null;

        } catch (Exception e) {

            log.warn("Error parsing expression: " + expression, e);
            return expression;
        }

    }
}
