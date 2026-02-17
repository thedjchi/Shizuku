// package moe.shizuku.manager.home

// import android.content.Intent
// import android.view.View
// import moe.shizuku.manager.starter.StarterActivity


//     private inline val start get() = binding.button1

//     init {
//         val listener = View.OnClickListener { v: View -> onStartClicked(v) }
//         start.setOnClickListener(listener)
//     }

//     private fun onStartClicked(v: View) {
//         val context = v.context
//         val intent = Intent(context, StarterActivity::class.java).apply {
//             putExtra(StarterActivity.EXTRA_IS_ROOT, true)
//         }
//         context.startActivity(intent)
//     }

// }
