import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.tp1application2.R

class LandingPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_landing_page, container, false)
        val loginButton = view.findViewById<Button>(R.id.login)
        val signupButton = view.findViewById<Button>(R.id.signup)

        loginButton.setOnClickListener {
            val loginFragment = LoginFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack(null)
                .commit()
        }

        signupButton.setOnClickListener {
            val signupFragment = SignupFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, signupFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}