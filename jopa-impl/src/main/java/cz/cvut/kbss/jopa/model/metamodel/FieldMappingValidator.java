/**
 * Copyright (C) 2020 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.model.metamodel;

import cz.cvut.kbss.jopa.exception.InvalidFieldMappingException;
import cz.cvut.kbss.jopa.model.annotations.OWLAnnotationProperty;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.utils.IdentifierTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static cz.cvut.kbss.jopa.utils.Constants.SUPPORTED_IDENTIFIER_TYPES;

/**
 * Verifies that a field's mapping metadata and declaration are valid.
 */
class FieldMappingValidator {

    void validatePropertiesField(Field field) {
        assert field != null;
        if (!Map.class.isAssignableFrom(field.getType())) {
            throw new InvalidFieldMappingException(
                    "Expected @Properties field to be a map, but it is a " + field.getType());
        }
        if (isRawType(field.getGenericType())) {
            throw new InvalidFieldMappingException("@Properties field cannot be a raw map.");
        }
        final PropertiesParametersResolver parametersResolver = new PropertiesParametersResolver(field);
        if (!isValidIdentifierType(parametersResolver.getKeyType())) {
            throw new InvalidFieldMappingException(
                    "@Properties key type is not a valid identifier type. Expected one of " +
                            SUPPORTED_IDENTIFIER_TYPES);
        }
        validatePropertiesValueType(parametersResolver.getValueType());
    }

    private static boolean isRawType(Type type) {
        return !(type instanceof ParameterizedType);
    }

    private static void validatePropertiesValueType(Type type) {
        if (isRawType(type)) {
            throw new InvalidFieldMappingException(
                    "Expected a java.util.Set as value parameter of the @Properties map, but got " + type);
        }
        if (!((ParameterizedType) type).getRawType().equals(Set.class)) {
            throw new InvalidFieldMappingException(
                    "Expected a java.util.Set as value parameter of the @Properties map, but got " + type);
        }
    }

    void validateTypesField(Field field) {
        if (!Set.class.isAssignableFrom(field.getType())) {
            throw new InvalidFieldMappingException("Expected @Types field to be a set, but it is a " + field.getType());
        }
        if (isRawType(field.getGenericType())) {
            throw new InvalidFieldMappingException("@Types field cannot be a raw set.");
        }
        final ParameterizedType typeSpec = (ParameterizedType) field.getGenericType();
        if (!isValidIdentifierType(typeSpec.getActualTypeArguments()[0])) {
            throw new InvalidFieldMappingException(
                    "@Types field value is not a valid identifier type. Expected one of " + SUPPORTED_IDENTIFIER_TYPES);
        }
    }

    void validateIdentifierType(Type type) {
        if (!isValidIdentifierType(type)) {
            throw new InvalidFieldMappingException(type + " is not a valid identifier type.");
        }
    }

    boolean isValidIdentifierType(Type type) {
        return type instanceof Class && IdentifierTransformer.isValidIdentifierType((Class<?>) type);
    }

    void validateAnnotationPropertyField(Field field, OWLAnnotationProperty config) {
        assert field != null;
        assert config != null;
        validateLexicalFormField(field, config.lexicalForm());
        validateSimpleLiteralField(field, config.simpleLiteral());
    }

    void validateDataPropertyField(Field field, OWLDataProperty config) {
        assert field != null;
        assert config != null;
        validateLexicalFormField(field, config.lexicalForm());
        validateSimpleLiteralField(field, config.simpleLiteral());
    }

    private static void validateLexicalFormField(Field field, boolean lexicalForm) {
        if (lexicalForm && !String.class.isAssignableFrom(getLiteralFieldType(field))) {
            throw new InvalidFieldMappingException("lexicalForm mapping can be used only on fields of type String.");
        }
    }

    private static void validateSimpleLiteralField(Field field, boolean simpleLiteral) {
        if (simpleLiteral && !String.class.isAssignableFrom(getLiteralFieldType(field))) {
            throw new InvalidFieldMappingException("simpleLiteral mapping can be used only on fields of type String.");
        }
    }

    private static Class<?> getLiteralFieldType(Field field) {
        final Class<?> fieldType = field.getType();
        if (List.class.isAssignableFrom(fieldType) || Set.class.isAssignableFrom(fieldType) ||
                SortedSet.class.isAssignableFrom(fieldType)) {
            final ParameterizedType typeSpec = (ParameterizedType) field.getGenericType();
            assert typeSpec.getActualTypeArguments().length == 1;
            return (Class<?>) typeSpec.getActualTypeArguments()[0];
        }
        return fieldType;
    }
}
