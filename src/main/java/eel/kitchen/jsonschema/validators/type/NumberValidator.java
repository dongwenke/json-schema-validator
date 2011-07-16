/*
 * Copyright (c) 2011, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eel.kitchen.jsonschema.validators.type;

import eel.kitchen.jsonschema.validators.AbstractValidator;
import eel.kitchen.jsonschema.validators.format.FormatValidator;
import eel.kitchen.jsonschema.validators.misc.EnumValidator;
import eel.kitchen.util.NodeType;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;

public final class NumberValidator
    extends AbstractValidator
{
    private static final BigDecimal ZERO = new BigDecimal("0");

    private BigDecimal minimum = null, maximum = null, divisor = null;
    private boolean exclusiveMinimum = false, exclusiveMaximum = false;

    public NumberValidator()
    {
        registerField("minimum", NodeType.INTEGER);
        registerField("minimum", NodeType.NUMBER);
        registerField("maximum", NodeType.INTEGER);
        registerField("maximum", NodeType.NUMBER);
        registerField("divisibleBy", NodeType.INTEGER);
        registerField("divisibleBy", NodeType.NUMBER);
        registerField("exclusiveMinimum", NodeType.BOOLEAN);
        registerField("exclusiveMaximum", NodeType.BOOLEAN);

        registerValidator(new EnumValidator());
        registerValidator(new FormatValidator());
    }

    @Override
    protected boolean doSetup()
    {
        JsonNode node;

        node = schema.path("minimum");

        if (node.isNumber())
            minimum = node.getDecimalValue();

        exclusiveMinimum = schema.path("exclusiveMinimum").getValueAsBoolean();

        node = schema.path("maximum");

        if (node.isNumber())
            maximum = node.getDecimalValue();

        exclusiveMaximum = schema.path("exclusiveMaximum").getValueAsBoolean();

        if (minimum != null && maximum != null) {
            final int tmp = minimum.compareTo(maximum);
            if (tmp > 0) {
                schemaErrors.add("minimum is greater than maximum");
                return false;
            }
            if (tmp == 0 && (exclusiveMinimum || exclusiveMaximum)) {
                schemaErrors.add("schema can never validate: minimum and maximum "
                    + "are equal but are excluded from matching");
                return false;
            }
        }

        node = schema.get("divisibleBy");

        if (node == null)
            return true;

        divisor = node.getDecimalValue();

        if (ZERO.compareTo(divisor) != 0)
            return true;

        schemaErrors.add("divisibleBy is 0");
        return false;
    }

    @Override
    protected boolean doValidate(final JsonNode node)
    {
        final BigDecimal value = node.getDecimalValue();

        int tmp;

        if (minimum != null) {
            tmp = value.compareTo(minimum);
            if (tmp < 0) {
                messages.add("number is strictly lower than the "
                    + "required minimum");
                return false;
            }
            if (tmp == 0 && exclusiveMinimum) {
                messages.add("number equals to the minimum, "
                    + "but should be strictly greater than it");
                return false;
            }
        }

        if (maximum != null) {
            tmp = value.compareTo(maximum);
            if (tmp > 0) {
                messages.add("number is strictly greater than the "
                    + "required maximum");
                return false;
            }
            if (tmp == 0 && exclusiveMaximum) {
                messages.add("number equals to the maximum, "
                    + "but should be strictly lower than it");
                return false;
            }
        }

        if (divisor == null)
            return true;

        if (ZERO.compareTo(value.remainder(divisor)) == 0)
            return true;

        messages.add("number is not a multiple of the declared divisor");
        return false;
    }
}
