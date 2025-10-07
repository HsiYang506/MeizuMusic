package com.jywkhyse.meizumusic.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.databinding.ActivityMainBinding
import com.jywkhyse.meizumusic.ui.search.SearchActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    // 将 appBarConfiguration 和 toolbar 提升为类属性，以便在其他方法中访问
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toolbar: Toolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 获取 Toolbar 引用
        toolbar = binding.appBarMain.toolbar

        // 2. ★★★ 关键步骤：在 setSupportActionBar 之前，为 Toolbar 应用动态主题
        setupToolbarTheme()

        // 3. 正常设置 Toolbar 和 Navigation
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_music, R.id.nav_album, R.id.nav_artist, R.id.nav_folder,
                R.id.nav_statistics, R.id.nav_settings, R.id.nav_about
            ),
            drawerLayout
        )

        // 现在，当这个方法执行时，它会从已经应用了新主题的 Toolbar 中创建DrawerToggle
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 4. 最后，设置状态栏等其他颜色
        setupStatusBar()
    }

    /**
     * 根据当前系统的亮/暗模式，为 Toolbar 应用一个主题叠加。
     * 这个方法必须在 setSupportActionBar() 之前调用。
     */
    private fun setupToolbarTheme() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            toolbar.context.theme.applyStyle(R.style.ThemeOverlay_App_Toolbar_Dark, true)
        } else {
            toolbar.context.theme.applyStyle(R.style.ThemeOverlay_App_Toolbar_Light, true)
        }
    }

    /**
     * 简化后的方法，只负责设置背景和状态栏
     */
    private fun setupStatusBar() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val bgColor: Int
        val contentColor: Int

        if (isNightMode) {
            bgColor = ContextCompat.getColor(this, R.color.md_grey_900)
            contentColor = Color.WHITE
        } else {
            bgColor = ContextCompat.getColor(this, R.color.md_grey_50)
            contentColor = Color.BLACK
        }

        supportActionBar?.setBackgroundDrawable(bgColor.toDrawable())
        toolbar.setTitleTextColor(contentColor)
        window.statusBarColor = bgColor

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

    }

    // onCreateOptionsMenu 方法现在可以简化
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}