package io.github.toyota32k.networkmonitor

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.bindit.LiteUnitCommand
import io.github.toyota32k.bindit.textBinding
import io.github.toyota32k.dialog.broker.pickers.UtCreateFilePicker
import io.github.toyota32k.dialog.task.UtImmortalSimpleTask
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.dialog.task.UtMortalActivity
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.UtLoggerInstance
import io.github.toyota32k.utils.bindCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : UtMortalActivity() {
    val fileCreator = UtCreateFilePicker().apply { register(this@MainActivity) }

    class MainViewModel(application:Application): AndroidViewModel(application) {
        val information = MutableStateFlow<String>("")
        val ticker = MutableStateFlow<Ticker?>(null)
        val clearCommand = LiteUnitCommand(this::clear)
        val checkPointCommand = LiteUnitCommand(this::addCheckPoint)
        val resetAllCommand = LiteUnitCommand(this::resetAll)
        val tickCommand = LiteUnitCommand(this::toggleTick)
        val tickButtonLabel = ticker.map { if(it!=null) "Stop Tick" else "Start Tick"}
        val logger = LogFile(application)

        private val counter = AtomicInteger(0)
        private var connectivityManager: ConnectivityManager? = application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        private val defCallback = DefCallback()
        init {
            connectivityManager?.registerDefaultNetworkCallback(defCallback)
        }
        override fun onCleared() {
            super.onCleared()
            connectivityManager?.unregisterNetworkCallback(defCallback)
            logger.close()
        }

        fun output(label:String, msg:String?=null) {
            val text = if(msg.isNullOrBlank()) label else "$label\n$msg"
            logger.info(text)
            information.value += "\n${logger.date}  $text\n\n"
        }

        inner class DefCallback : NetworkCallback() {
            override fun onAvailable(network: Network) {
                output("onAvailable", null)
                super.onAvailable(network)
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                output("onBlockedStatusChanged", "blocked=$blocked")
                super.onBlockedStatusChanged(network, blocked)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                output("onCapabilitiesChanged", "networkCapabilities=$networkCapabilities")
                super.onCapabilitiesChanged(network, networkCapabilities)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                output("onLinkPropertiesChanged", "linkProperties=$linkProperties")
                super.onLinkPropertiesChanged(network, linkProperties)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                output("onLosing", "maxMsToLive=$maxMsToLive")
                super.onLosing(network, maxMsToLive)
            }
            override fun onUnavailable() {
                output("onUnavailable", null)
                super.onUnavailable()
            }
            override fun onLost(network: Network) {
                output("onLost", null)
                super.onLost(network)
            }

        }

        private fun addCheckPoint() {
            val msg = "------- ${counter.incrementAndGet()}"
            logger.info(msg)
            information.value += "${logger.date}  $msg\n"
        }

        private fun clear() {
            information.value = ""
        }

        private fun resetAll() {
            logger.clearAll()
            clear()
        }

        val pool = Executors.newSingleThreadExecutor()
        inner class Ticker : Runnable {
            var alive:Boolean = true
            var tick = 0
            override fun run() {
                while(alive) {
                    val msg = "tick-${++tick}"
                    logger.info(msg)
                    information.value +="${logger.date}  $msg\n"
                    Thread.sleep(1000)
                }
            }
        }

        private fun toggleTick() {
            if(ticker.value==null) {
                ticker.value = Ticker().apply {
                    pool.execute(this)
                }
            } else {
                ticker.value?.alive = false
                ticker.value = null
            }
        }
    }

    private val viewModel by viewModels<MainViewModel>()

    val binder = Binder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel.output("onCreate")
        binder
            .owner(this)
            .add(ScrollTextBinding(viewModel.information.asLiveData()).apply { connect(this@MainActivity, findViewById(R.id.information)) })
            .bindCommand(viewModel.clearCommand, findViewById(R.id.button_clear))
            .bindCommand(viewModel.checkPointCommand, findViewById(R.id.button_check))
            .bindCommand(viewModel.resetAllCommand, findViewById(R.id.button_reset_all))
            .bindCommand(viewModel.tickCommand, findViewById(R.id.button_tick))
            .textBinding(findViewById(R.id.button_tick), viewModel.tickButtonLabel)
            .bindCommand(LiteUnitCommand(this::saveLog), findViewById(R.id.button_save_log))
    }

    private fun saveLog() {
        UtImmortalSimpleTask.run {
            val uri = fileCreator.selectFile("log.txt", "text/plain")
            if(uri!=null) {
                withOwner {
                    (it.asActivity() as MainActivity).viewModel.logger.export(uri)
                }
            }
            true
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        viewModel.output("onContentChanged")
    }

    override fun onStart() {
        super.onStart()
        viewModel.output("onStart")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.output("onRestart")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        viewModel.output("onRestoreInstanceState")
    }

    override fun onResume() {
        super.onResume()
        viewModel.output("onResume")
    }

    override fun onPause() {
        super.onPause()
        viewModel.output("onPause")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        viewModel.output("onSaveInstanceState")
    }

    override fun onStop() {
        super.onStop()
        viewModel.output("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.output("onDestroy")
    }
}