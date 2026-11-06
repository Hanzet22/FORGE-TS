package com.HG.Forge.Ts.data.remote
/** Supabase-backed remote boundary; privileged keys must never ship in the APK. */
interface RemoteDataSource { suspend fun isAvailable():Boolean }
