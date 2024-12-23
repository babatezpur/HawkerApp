import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hawkerapp.app.R
import com.hawkerapp.app.adapters.HawkerAdapter
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.repositories.HawkerRelatedApis
import com.hawkerapp.app.viewmodels.UserViewModel
import com.hawkerapp.app.views.UserViewActivity

class HawkerSearchBottomSheet (
    private var appContext: Context,
    private val viewModel: UserViewModel
) : BottomSheetDialogFragment() {
    private lateinit var closeButton: ImageButton
    private lateinit var hawkerRecyclerView: RecyclerView
    private lateinit var hawkerAdapter: HawkerAdapter
    private var pendingHawkers: List<HawkerInfo>? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hawker_search_bottom_sheet, container, false)

        // Initialize views
        hawkerRecyclerView = view.findViewById(R.id.hawkerRecyclerView)
        closeButton = view.findViewById(R.id.closeButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCloseButton()


        pendingHawkers?.let {
            updateHawkersList(it)
            pendingHawkers = null
        }
    }



    private fun setupRecyclerView() {
        hawkerAdapter = HawkerAdapter { hawkerInfo ->
            Log.d("BottomSheet","Hawkers in setupView of BottomSheet: ${hawkerInfo.name}")
            // Use the same details display function
            viewModel.setSelectedHawker(hawkerInfo)
            dismiss() // Dismiss the search results sheet
        }

        // Set up RecyclerView
        hawkerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hawkerAdapter
            // Optional: Add item decoration for dividers
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupCloseButton() {
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    // Public method to update the list from outside
    fun updateHawkersList(hawkers: List<HawkerInfo>) {
        Log.d("BottomSheet","Hawkers adapter isInitialized: ${::hawkerAdapter.isInitialized}")
        if (::hawkerAdapter.isInitialized) {
            hawkerAdapter.updateHawkers(hawkers)
        } else {
            pendingHawkers = hawkers
        }
    }
}