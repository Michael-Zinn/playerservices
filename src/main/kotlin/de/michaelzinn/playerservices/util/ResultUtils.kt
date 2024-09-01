package de.michaelzinn.playerservices.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold

@Suppress("FunctionName")
fun Err(): Result<Any?, Unit> = Err(Unit)

@Suppress("FunctionName")
fun Ok() = Ok(Unit)

fun Result<*, *>.toBoolean() = this.fold(success = { true }, failure = { false })