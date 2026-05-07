package com.blocker.meituan

interface IShellService {
    fun runCommand(cmd: String): String
}

class ShellService : IShellService {
    override fun runCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }
}
