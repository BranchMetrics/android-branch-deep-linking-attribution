package io.branch.referral

import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory SharedPreferences for JVM tests. Like the real Editor, clear() is applied
 * before the puts and removes made on the same Editor, and pending changes reset after apply().
 */
class FakeSharedPreferences : SharedPreferences {
    private val values = ConcurrentHashMap<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(values)
    override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (values[key] as MutableSet<String>?) ?: defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = HashMap<String, Any?>()
        private var clearPending = false

        private fun put(key: String, value: Any?): SharedPreferences.Editor {
            synchronized(this) { pending[key] = value }
            return this
        }

        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: MutableSet<String>?) = put(key, values)
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)
        override fun remove(key: String) = put(key, null)
        override fun clear(): SharedPreferences.Editor {
            synchronized(this) { clearPending = true }
            return this
        }
        override fun commit(): Boolean { apply(); return true }

        override fun apply() {
            synchronized(this) {
                if (clearPending) values.clear()
                for ((key, value) in pending) {
                    if (value == null) values.remove(key) else values[key] = value
                }
                pending.clear()
                clearPending = false
            }
        }
    }
}
