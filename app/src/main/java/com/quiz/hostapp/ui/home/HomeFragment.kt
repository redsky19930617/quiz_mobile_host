package com.quiz.hostapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.quiz.hostapp.MyApplication
import com.quiz.hostapp.ServiceLocator
import com.quiz.hostapp.databinding.FragmentHomeBinding
import com.quiz.hostapp.network.model.FreeQuiz
import com.quiz.hostapp.network.model.QuizResult

class HomeFragment : Fragment(), UpComingQuizlistAdapter.OnQuizItemClickListener {

    private lateinit var upComingQuizlistAdapter: UpComingQuizlistAdapter
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var viewModel: HomeViewModel
    private val overviewAdapter = FreeQuizListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModelFactory = ServiceLocator.provideViewModelFactory(
            requireContext(),
            ServiceLocator.provideApiService(requireContext())
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java].apply {
            getUpComingQuizzes()
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        upComingQuizlistAdapter = UpComingQuizlistAdapter(this)
        binding.upcomingRecyclerView.adapter = upComingQuizlistAdapter

        binding.freeQuizRecyclerView.adapter = overviewAdapter
        binding.profileButton.setOnClickListener {
//            checkLoginType()
            MyApplication.session(requireContext()).deleteSaveData()
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToLoginFragment())
        }
        registerObservers()
    }

    private fun registerObservers() {
        //Observing upcoming quizzes
        viewModel.upcomingQuizLiveData.observe(viewLifecycleOwner) {
            it?.let { result: Result<List<QuizResult>?> ->
                if (result.isSuccess) {
                    val data: List<QuizResult>? = result.getOrNull()
                    data?.let { quizList ->
                        Log.e(TAG, "no of quiz: ${quizList.size}")
                        upComingQuizlistAdapter.submitList(quizList)
                    } ?: Log.e(TAG, "quiz list is null")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to get data in ui: $error")
                }
            }
        }

        viewModel.quizOverviewLiveData.observe(viewLifecycleOwner) {
            it?.let { result: Result<List<FreeQuiz>?> ->
                if (result.isSuccess) {
                    val data: List<FreeQuiz>? = result.getOrNull()
                    data?.let { quizList ->
                        overviewAdapter.submitList(quizList)
                    } ?: Log.e(TAG, "overview quiz list is null")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to get data in ui: $error")
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onQuizItemClick(quizDetails: QuizResult, isVoting: Boolean) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToWaitingFragment(
                quizDetails.id,
                quizDetails.category ?: "",
                quizDetails.startDate
            )
        )
    }
}