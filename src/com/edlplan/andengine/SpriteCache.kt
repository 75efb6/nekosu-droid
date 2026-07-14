package com.edlplan.andengine

import com.edlplan.framework.utils.Factory
import java.util.Stack

class SpriteCache {

    class Cache<T>(private val maxCacheCount: Int, private val constructor: Factory<T>) {

        private val stack = Stack<T>()

        fun save(t: T) {
            if (stack.size < maxCacheCount) {
                stack.push(t)
            }
        }

        fun get(): T {
            return if (stack.isEmpty()) {
                constructor.create()
            } else {
                stack.pop()
            }
        }
    }

    companion object {
        @JvmField
        val trianglePackCache = Cache(100) { TrianglePack() }
    }
}
