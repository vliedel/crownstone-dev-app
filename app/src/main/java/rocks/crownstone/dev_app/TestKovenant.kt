package rocks.crownstone.dev_app

import android.os.AsyncTask
import android.util.Log
import nl.komponents.kovenant.*

class TestKovenant {
	val TAG = TestKovenant::class.java.canonicalName

	fun test() {
		Log.i(TAG, "test")
		fun1(1)
				.then {
					Log.i(TAG, "1: $it")
					fun2(2)
				}.unwrap()
				.then {
					Log.i(TAG, "2: $it")

					val deferred = deferred<String, Exception>()
					fun2(-2)
							.success {
								Log.i(TAG, "nested then: $it")
								deferred.resolve(it)
							}.fail {
								Log.i(TAG, "nested fail: ${it.message}")
								deferred.resolve("recovered")
							}
					deferred.promise

				}.unwrap()
				.then {
					Log.i(TAG, "-2: $it")
					fun1(-1)
				}.unwrap()
				.then {
					Log.i(TAG, "-1: $it")
				}
				.fail {
					Log.e(TAG, "FAIL: " + it.message)
				}
				.always {
					Log.i(TAG, "always")
				}

		Log.i(TAG, "test2")
		test2(1)
				.success { Log.i(TAG, "test2 success") }
				.fail { Log.i(TAG, "test2 fail") }
		test2(-1)
				.success { Log.i(TAG, "test2 success") }
				.fail { Log.i(TAG, "test2 fail") }


		Log.i(TAG, "all")
		all(fun1(1), fun2(1), fun1(1))
				.success { Log.i(TAG, "all success") }
				.fail { Log.i(TAG, "all fail") }
				.always { Log.i(TAG, "all always") }

		Log.i(TAG, "all2")
		all(fun1(1), fun2(-1), fun1(1))
				.success { Log.i(TAG, "all2 success") }
				.fail { Log.i(TAG, "all2 fail") }
				.always { Log.i(TAG, "all2 always") }

		val promise3 = fun3(1)
				.success { Log.i(TAG, "test3 success $it") }
				.fail { Log.i(TAG, "test3 fail $it") }
				.always { Log.i(TAG, "test3 always") }
		Kovenant.cancel(promise3, Exception("test3 cancel"))
	}

	fun test2(nr: Int): Promise<Unit, Exception> {
		Log.i(TAG, "test2 start")
		val deferred = deferred<Unit, Exception>()
		val promise = deferred.promise
		fun1(nr)
				.then {
					Log.i(TAG, "test2 fun1 success")
					deferred.resolve()
				}
				.fail {
					Log.i(TAG, "test2 fun1 fail")
					deferred.reject(it)
				}
		return promise
	}

	private abstract class AsyncDummy : AsyncTask<Int, Void, String>() {
		override fun doInBackground(vararg p0: Int?): String {
			Thread.sleep(1000)
			for (nr in p0) {
				if (nr == null || nr < 0)
					return "Failed"
			}
			return "Success"
		}
	}

	fun fun1(nr: Int): Promise<String, Exception> {
		Log.i(TAG, "fun1 start nr=$nr")
		val deferred = deferred<String, Exception>()
		if (nr < 0) {
			deferred.reject(Exception("fun1 needs some positivity!"))
		}
		else {
			Log.i(TAG, "fun1 success")
			deferred.resolve(nr.toString())
		}
		return deferred.promise
	}

	fun fun2(nr: Int): Promise<String, Exception> {
		Log.i(TAG, "fun2 start nr=$nr")
		val deferred = deferred<String, Exception>()

		class DummyTask : AsyncDummy() {
			override fun onPostExecute(result: String?) {
				super.onPostExecute(result)
				if (result.equals("Failed")) {
					deferred.reject(Exception("fun2: $result"))
					return
				}
				Log.i(TAG, "fun2 success: $result")
				deferred.resolve("Done")
			}
		}
		val task = DummyTask()
		task.execute(nr)

		return deferred.promise
	}

	fun fun3(nr: Int): Promise<String, Exception> {
		Log.i(TAG, "fun3 start nr=$nr")
		val deferred = deferred<String, Exception> {
			Log.i(TAG, "fun3 is being canceled! $it")
		}

		class DummyTask : AsyncDummy() {
			override fun onPostExecute(result: String?) {
				super.onPostExecute(result)
				if (result.equals("Failed")) {
					deferred.reject(Exception("fun3: $result"))
					return
				}
				Log.i(TAG, "fun3 success: $result")
				deferred.resolve("Done")
			}
		}
		val task = DummyTask()
		task.execute(nr)

		return deferred.promise
	}

}
