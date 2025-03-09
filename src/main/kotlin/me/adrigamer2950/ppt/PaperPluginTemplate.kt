package me.adrigamer2950.ppt

import me.adrigamer2950.adriapi.api.APIPlugin


class PaperPluginTemplate : APIPlugin() {

    override fun onPreLoad() {
        val preLoadTime = System.currentTimeMillis()

        logger.info("&6Loaded in ${System.currentTimeMillis() - preLoadTime}ms")
    }

    override fun onPostLoad() {
        val postLoadTime = System.currentTimeMillis()

        logger.info("&aEnabled in ${System.currentTimeMillis() - postLoadTime}ms")
    }

    override fun onUnload() {
        logger.info("&cDisabled")
    }

}
