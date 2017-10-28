package mustafaozhan.github.com.mycurrencies.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import mustafaozhan.github.com.mycurrencies.R
import mustafaozhan.github.com.mycurrencies.model.data.Currency
import android.graphics.drawable.Drawable


/**
 * Created by Mustafa Ozhan on 10/7/17 at 6:56 PM on Arch Linux.
 */
class MyCurrencyAdapter(private val currencyList: ArrayList<Currency>?, private val context: Context) : RecyclerView.Adapter<MyCurrencyAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var type: TextView = view.findViewById(R.id.txtType)
        var amount: TextView = view.findViewById(R.id.txtAmount)
        var view: View = view.findViewById(R.id.view)
        var icon: ImageView = view.findViewById(R.id.txtIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.row, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currency = currencyList!![position]
        holder.type.text = currency.name
        holder.amount.text = (Math.floor(currency.rate * 100) / 100).toString()

        var mDrawableName = holder.type.text.toString().toLowerCase()
        if (mDrawableName == "try")
            mDrawableName = "tryy"
        val id = context.resources.getIdentifier(mDrawableName, "drawable", context.packageName)
        val drawable = context.resources.getDrawable(id)
        holder.icon.setImageDrawable(drawable)




//        holder.title.setText(movie.getTitle())
//        holder.genre.setText(movie.getGenre())
//        holder.year.setText(movie.getYear())
    }

    override fun getItemCount(): Int = currencyList?.size ?: -1
}
