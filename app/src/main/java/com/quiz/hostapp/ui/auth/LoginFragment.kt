package com.quiz.hostapp.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.ServiceLocator
import com.quiz.hostapp.databinding.FragmentLoginBinding
import com.quiz.hostapp.network.apiService
import com.quiz.hostapp.network.model.LoginResponse
import com.quiz.hostapp.utils.SessionManager
import com.quiz.hostapp.utils.hideKeyboard

class LoginFragment : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance() = LoginFragment()
        private const val TAG = "LoginFragment"
    }

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentLoginBinding

    private lateinit var emailNumber: String
    private lateinit var password: String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModelFactory =
            ServiceLocator.provideViewModelFactory(requireContext(), apiService(requireContext()))

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        setClicks()
        registerObservers()
    }

    private fun registerObservers() {
        viewModel.loginLiveData.observe(viewLifecycleOwner) {
            it?.let { result: Result<LoginResponse> ->
                loading(false)
                if (result.isSuccess) {
                    if (binding.rememberMeCheckBox.isChecked) {
                        saveCredential(result.getOrNull())
                    }
                    goToHome(result.getOrNull())
                } else {
                    Log.e(TAG, "error: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goToHome(response: LoginResponse?) {
        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToHomeFragment())
    }

    private fun loading(isLoading: Boolean) {
        binding.loginButton.isVisible = isLoading.not()
        binding.loginProgressBar.isVisible = isLoading
    }

    private fun setClicks() {
        binding.loginButton.setOnClickListener(this)
        binding.forgotPassTv.setOnClickListener(this)
    }

    private fun saveCredential(loginResponse: LoginResponse?) {

        session(requireContext()).savePrefBool(SessionManager.IS_LOGIN, true)
        loginResponse?.let {
            session(requireContext()).savePrefString(SessionManager.USER_TYPE, it.user.role)
            session(requireContext()).savePrefString(
                SessionManager.ACCESS_TOKEN,
                loginResponse.tokens.access.token
            )
            session(requireContext()).savePrefString(
                SessionManager.REFRESH_TOKEN,
                loginResponse.tokens.refresh.token
            )
            session(requireContext()).savePrefString(
                SessionManager.ACCESS_TOKEN_TIME,
                loginResponse.tokens.access.expires
            )
            session(requireContext()).savePrefString(SessionManager.USER_ID, loginResponse.user.id)
        }
    }

    private fun validateInputs() {
        val emailNumber = binding.editTextTextEmailAddress.text.toString()
        val password = binding.editTextTextPassword.text.toString()

        if (emailNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Enter Email or Mobile Number", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Enter Password", Toast.LENGTH_SHORT).show()
            return
        }
        HashMap<String, String>().apply {
            put("email", emailNumber)
            put("password", password)
            viewModel.loginUser(this)
        }
    }

    override fun onClick(p0: View?) {
        when (p0) {
            binding.loginButton -> {
                hideKeyboard()
                loading(true)
                validateInputs()
                loading(false)
            }
        }
    }

}