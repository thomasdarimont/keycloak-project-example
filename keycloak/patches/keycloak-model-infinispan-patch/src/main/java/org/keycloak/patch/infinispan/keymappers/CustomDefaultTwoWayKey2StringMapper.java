package org.keycloak.patch.infinispan.keymappers;

import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Base64;
import java.util.UUID;

/**
 * Patched version of {@link org.infinispan.persistence.keymappers.DefaultTwoWayKey2StringMapper} with added support
 * for {@link UUID UUID's}. This allows us to store clientSessions cache entries with the infinispan jdbc-store.
 *
 * @since 4.1
 */
public class CustomDefaultTwoWayKey2StringMapper implements TwoWayKey2StringMapper {
    private static final Log log = LogFactory.getLog(CustomDefaultTwoWayKey2StringMapper.class);

    private static final char NON_STRING_PREFIX = '\uFEFF';
    private static final char SHORT_IDENTIFIER = '1';
    private static final char BYTE_IDENTIFIER = '2';
    private static final char LONG_IDENTIFIER = '3';
    private static final char INTEGER_IDENTIFIER = '4';
    private static final char DOUBLE_IDENTIFIER = '5';
    private static final char FLOAT_IDENTIFIER = '6';
    private static final char BOOLEAN_IDENTIFIER = '7';
    private static final char BYTEARRAYKEY_IDENTIFIER = '8';
    private static final char NATIVE_BYTEARRAYKEY_IDENTIFIER = '9';

    // PATCH:Begin
    private static final char UUID_IDENTIFIER = '9' + 1;
    // PATCH:END

    @Override
    public String getStringMapping(Object key) {
        char identifier;
        if (key.getClass().equals(String.class)) {
            return key.toString();
        } else if (key.getClass().equals(Short.class)) {
            identifier = SHORT_IDENTIFIER;
        } else if (key.getClass().equals(Byte.class)) {
            identifier = BYTE_IDENTIFIER;
        } else if (key.getClass().equals(Long.class)) {
            identifier = LONG_IDENTIFIER;
        } else if (key.getClass().equals(Integer.class)) {
            identifier = INTEGER_IDENTIFIER;
        } else if (key.getClass().equals(Double.class)) {
            identifier = DOUBLE_IDENTIFIER;
        } else if (key.getClass().equals(Float.class)) {
            identifier = FLOAT_IDENTIFIER;
        } else if (key.getClass().equals(Boolean.class)) {
            identifier = BOOLEAN_IDENTIFIER;
        } else if (key.getClass().equals(WrappedByteArray.class)) {
            return generateString(BYTEARRAYKEY_IDENTIFIER, Base64.getEncoder().encodeToString(((WrappedByteArray) key).getBytes()));
        } else if (key.getClass().equals(byte[].class)) {
            return generateString(NATIVE_BYTEARRAYKEY_IDENTIFIER, Base64.getEncoder().encodeToString((byte[]) key));
        }
        // PATCH:Begin
        else if (key.getClass().equals(UUID.class)) {
            identifier = UUID_IDENTIFIER;
        }
        // PATCH:End
        else {
            throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
        }
        return generateString(identifier, key.toString());
    }

    @Override
    public Object getKeyMapping(String key) {
        log.tracef("Get mapping for key: %s", key);
        if (key.length() > 0 && key.charAt(0) == NON_STRING_PREFIX) {
            char type = key.charAt(1);
            String value = key.substring(2);
            switch (type) {
                case SHORT_IDENTIFIER:
                    return Short.parseShort(value);
                case BYTE_IDENTIFIER:
                    return Byte.parseByte(value);
                case LONG_IDENTIFIER:
                    return Long.parseLong(value);
                case INTEGER_IDENTIFIER:
                    return Integer.parseInt(value);
                case DOUBLE_IDENTIFIER:
                    return Double.parseDouble(value);
                case FLOAT_IDENTIFIER:
                    return Float.parseFloat(value);
                case BOOLEAN_IDENTIFIER:
                    return Boolean.parseBoolean(value);
                // PATCH:Begin
                case UUID_IDENTIFIER:
                    return UUID.fromString(value);
                // PATCH:End
                case BYTEARRAYKEY_IDENTIFIER:
                    byte[] bytes = Base64.getDecoder().decode(value);
                    return new WrappedByteArray(bytes);
                case NATIVE_BYTEARRAYKEY_IDENTIFIER:
                    return Base64.getDecoder().decode(value);
                default:
                    throw new IllegalArgumentException("Unsupported type code: " + type);
            }
        } else {
            return key;
        }
    }

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return isPrimitive(keyType) || keyType == WrappedByteArray.class;
    }

    private String generateString(char identifier, String s) {
        return NON_STRING_PREFIX + String.valueOf(identifier) + s;
    }

    private static boolean isPrimitive(Class<?> key) {
        return key == String.class || key == Short.class || key == Byte.class || key == Long.class || key == Integer.class
                || key == Double.class || key == Float.class || key == Boolean.class || key == byte[].class
                // PATCH:Begin
                || key == UUID.class
                // PATCH:End
                ;
    }
}