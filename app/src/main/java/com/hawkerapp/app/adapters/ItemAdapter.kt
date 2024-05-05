import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.hawkerapp.app.models.Item

class ItemAdapter(private val itemsList: ArrayList<Item>) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_input_layout, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = itemsList[position]
        holder.itemName.setText(currentItem.name)
        holder.itemPrice.setText(currentItem.price.toString())
        holder.itemQuantity.setText(currentItem.quantity.toString())
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Initialize views from item_row.xml if needed
        val itemName: EditText = itemView.findViewById(R.id.itemName)
        val itemPrice: EditText = itemView.findViewById(R.id.itemPrice)
        val itemQuantity: EditText = itemView.findViewById(R.id.itemQuantity)
    }
}