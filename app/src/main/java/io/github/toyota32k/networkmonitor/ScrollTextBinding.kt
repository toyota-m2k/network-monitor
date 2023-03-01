package io.github.toyota32k.networkmonitor

import android.widget.EditText
import androidx.lifecycle.LiveData
import io.github.toyota32k.bindit.BindingMode
import io.github.toyota32k.bindit.TextBinding

class ScrollTextBinding(data: LiveData<String>) : TextBinding(data, BindingMode.OneWay) {
    override fun onDataChanged(v: String?) {
        super.onDataChanged(v)
        (textView as? EditText)?.apply {
            setSelection(v?.length?:0)
//            scrollTo(0, (lineCount-1)*lineHeight)
        }
    }
}