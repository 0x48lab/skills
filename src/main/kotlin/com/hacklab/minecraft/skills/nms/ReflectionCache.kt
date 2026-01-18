package com.hacklab.minecraft.skills.nms

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Centralized cache for reflection lookups.
 * Provides utilities for finding classes, methods, and fields with caching.
 */
object ReflectionCache {

    private var logger: Logger? = null
    private val classCache = ConcurrentHashMap<String, Class<*>?>()
    private val methodCache = ConcurrentHashMap<String, Method?>()
    private val fieldCache = ConcurrentHashMap<String, Field?>()

    /**
     * Initialize with logger.
     */
    fun initialize(logger: Logger) {
        this.logger = logger
    }

    /**
     * Find a class by trying multiple possible names.
     * Results are cached.
     *
     * @param cacheKey Unique key for caching
     * @param classNames List of possible class names to try
     * @return The found class or null
     */
    fun findClass(cacheKey: String, vararg classNames: String): Class<*>? {
        classCache[cacheKey]?.let { return it }

        for (name in classNames) {
            try {
                val clazz = Class.forName(name)
                classCache[cacheKey] = clazz
                logger?.fine("[NMS] Found class $cacheKey: $name")
                return clazz
            } catch (_: ClassNotFoundException) {
                // Try next
            }
        }

        classCache[cacheKey] = null
        logger?.warning("[NMS] Could not find class $cacheKey. Tried: ${classNames.joinToString()}")
        return null
    }

    /**
     * Find a class using the CraftBukkit package prefix.
     */
    fun findCraftBukkitClass(cacheKey: String, subPath: String): Class<*>? {
        val version = NmsVersion.detect()
        val packageBase = version.getCraftBukkitPackage()

        // Try unversioned first (Paper 1.20.5+), then versioned
        return findClass(
            cacheKey,
            "$packageBase.$subPath",
            "org.bukkit.craftbukkit.v1_21_R3.$subPath",
            "org.bukkit.craftbukkit.v1_21_R2.$subPath",
            "org.bukkit.craftbukkit.v1_21_R1.$subPath"
        )
    }

    /**
     * Find a method by name with specific parameter types.
     * Results are cached.
     */
    fun findMethod(
        cacheKey: String,
        clazz: Class<*>,
        methodName: String,
        vararg paramTypes: Class<*>
    ): Method? {
        methodCache[cacheKey]?.let { return it }

        try {
            val method = clazz.getMethod(methodName, *paramTypes)
            method.isAccessible = true
            methodCache[cacheKey] = method
            logger?.fine("[NMS] Found method $cacheKey: $methodName")
            return method
        } catch (_: NoSuchMethodException) {
            // Try declared methods
            try {
                val method = clazz.getDeclaredMethod(methodName, *paramTypes)
                method.isAccessible = true
                methodCache[cacheKey] = method
                logger?.fine("[NMS] Found declared method $cacheKey: $methodName")
                return method
            } catch (_: NoSuchMethodException) {
                methodCache[cacheKey] = null
                logger?.warning("[NMS] Could not find method $cacheKey: $methodName")
                return null
            }
        }
    }

    /**
     * Find a method by trying multiple possible names.
     */
    fun findMethodByNames(
        cacheKey: String,
        clazz: Class<*>,
        methodNames: List<String>,
        vararg paramTypes: Class<*>
    ): Method? {
        methodCache[cacheKey]?.let { return it }

        for (name in methodNames) {
            try {
                val method = clazz.getMethod(name, *paramTypes)
                method.isAccessible = true
                methodCache[cacheKey] = method
                logger?.fine("[NMS] Found method $cacheKey: $name")
                return method
            } catch (_: NoSuchMethodException) {
                // Try next
            }
        }

        // Try declared methods
        for (name in methodNames) {
            try {
                val method = clazz.getDeclaredMethod(name, *paramTypes)
                method.isAccessible = true
                methodCache[cacheKey] = method
                logger?.fine("[NMS] Found declared method $cacheKey: $name")
                return method
            } catch (_: NoSuchMethodException) {
                // Try next
            }
        }

        methodCache[cacheKey] = null
        return null
    }

    /**
     * Find a method by parameter signature (when name is obfuscated).
     */
    fun findMethodBySignature(
        cacheKey: String,
        clazz: Class<*>,
        returnType: Class<*>?,
        vararg paramTypes: Class<*>
    ): Method? {
        methodCache[cacheKey]?.let { return it }

        for (method in clazz.declaredMethods) {
            val paramsMatch = method.parameterTypes.contentEquals(paramTypes)
            val returnMatches = returnType == null || method.returnType == returnType

            if (paramsMatch && returnMatches) {
                method.isAccessible = true
                methodCache[cacheKey] = method
                logger?.fine("[NMS] Found method by signature $cacheKey: ${method.name}")
                return method
            }
        }

        methodCache[cacheKey] = null
        return null
    }

    /**
     * Find a method that has a specific parameter type (for finding obfuscated methods).
     */
    fun findMethodWithParamType(
        cacheKey: String,
        clazz: Class<*>,
        paramType: Class<*>,
        paramCount: Int? = null
    ): Method? {
        methodCache[cacheKey]?.let { return it }

        for (method in clazz.declaredMethods) {
            val hasParamType = method.parameterTypes.any { it == paramType }
            val countMatches = paramCount == null || method.parameterCount == paramCount

            if (hasParamType && countMatches) {
                method.isAccessible = true
                methodCache[cacheKey] = method
                logger?.fine("[NMS] Found method with param type $cacheKey: ${method.name}")
                return method
            }
        }

        methodCache[cacheKey] = null
        return null
    }

    /**
     * Find a field by name.
     */
    fun findField(cacheKey: String, clazz: Class<*>, fieldName: String): Field? {
        fieldCache[cacheKey]?.let { return it }

        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            fieldCache[cacheKey] = field
            logger?.fine("[NMS] Found field $cacheKey: $fieldName")
            return field
        } catch (_: NoSuchFieldException) {
            fieldCache[cacheKey] = null
            logger?.warning("[NMS] Could not find field $cacheKey: $fieldName")
            return null
        }
    }

    /**
     * Find a field by type (when name is obfuscated).
     */
    fun findFieldByType(cacheKey: String, clazz: Class<*>, fieldType: Class<*>): Field? {
        fieldCache[cacheKey]?.let { return it }

        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            for (field in currentClass.declaredFields) {
                if (field.type == fieldType) {
                    field.isAccessible = true
                    fieldCache[cacheKey] = field
                    logger?.fine("[NMS] Found field by type $cacheKey: ${field.name}")
                    return field
                }
            }
            currentClass = currentClass.superclass
        }

        fieldCache[cacheKey] = null
        return null
    }

    /**
     * Find a field by type name pattern (for finding obfuscated fields).
     */
    fun findFieldByTypeName(cacheKey: String, clazz: Class<*>, typeNameContains: String): Field? {
        fieldCache[cacheKey]?.let { return it }

        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            for (field in currentClass.declaredFields) {
                if (field.type.simpleName.contains(typeNameContains)) {
                    field.isAccessible = true
                    fieldCache[cacheKey] = field
                    logger?.fine("[NMS] Found field by type name $cacheKey: ${field.name} (${field.type.simpleName})")
                    return field
                }
            }
            currentClass = currentClass.superclass
        }

        fieldCache[cacheKey] = null
        return null
    }

    /**
     * Get enum constant by index.
     */
    fun getEnumConstant(enumClass: Class<*>, index: Int): Any? {
        val constants = enumClass.enumConstants ?: return null
        return if (index < constants.size) constants[index] else null
    }

    /**
     * Clear all caches (useful for reload).
     */
    fun clearCaches() {
        classCache.clear()
        methodCache.clear()
        fieldCache.clear()
    }

    /**
     * Get cache statistics for debugging.
     */
    fun getStats(): String {
        return buildString {
            appendLine("ReflectionCache Statistics:")
            appendLine("  Classes cached: ${classCache.size}")
            appendLine("  Methods cached: ${methodCache.size}")
            appendLine("  Fields cached: ${fieldCache.size}")
        }
    }
}
