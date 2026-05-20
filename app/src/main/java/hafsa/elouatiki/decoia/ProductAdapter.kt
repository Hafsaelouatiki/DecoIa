package hafsa.elouatiki.decoia

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val items: List<Mobilier>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val container: LinearLayout, val tvName: TextView, val tvSearch: TextView, val tvLink: TextView) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val context = parent.context
        val tvName = TextView(context).apply { textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
        val tvSearch = TextView(context).apply { textSize = 14f; setPadding(0, 8, 0, 4) }
        val tvLink = TextView(context).apply { textSize = 14f; text = "Voir produit" }
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 20)
            addView(tvName)
            addView(tvSearch)
            addView(tvLink)
        }
        return ProductViewHolder(container, tvName, tvSearch, tvLink)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.nom ?: "Recommandation"
        holder.tvSearch.text = item.recherche
        
        val link = item.shoppingResult?.link
        holder.tvLink.visibility = if (link.isNullOrBlank()) ViewGroup.GONE else ViewGroup.VISIBLE
        holder.container.setOnClickListener {
            link?.let { holder.itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
        }
    }

    override fun getItemCount() = items.size
}
