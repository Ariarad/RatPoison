package rat.poison.ui.tabs

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.Validators
import com.kotcrab.vis.ui.util.adapter.ArrayListAdapter
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import rat.poison.SETTINGS_DIRECTORY
import rat.poison.curSettings
import rat.poison.scripts.forcedUpdate
import rat.poison.scripts.skinChanger
import rat.poison.toSkinWeaponClass
import rat.poison.ui.changed
import rat.poison.ui.uiHelpers.VisCheckBoxCustom
import java.io.File

class SkinChangerTab : Tab(false, false) {
    private val table = VisTable(true)

    //Init labels/sliders/boxes that show values here
    private var enableSkinChanger = VisCheckBoxCustom("Enable Skinchanger", "SKINCHANGER")
    private var enableKnifeChanger = VisCheckBoxCustom("Enable Knifechanger", "KNIFECHANGER")
    private var warningLabel = VisLabel("This is experimental and is not what the completed\n" +
            " version will look like just a temporary version to show\n" +
            "it works, updates are in progress, and to report any issues.\n" +
            "To find the correct ID use csgostash.com and use the\n" +
            "'Finish Catalog' number.\n\n" +
            "May be cpu intensive. When enabled, press force update\n" +
            "to apply skins.")

    private var categorySelectionBox = VisSelectBox<String>()
    private var weaponSelectionBox = VisSelectBox<String>()
    private var knifeSelectionBox = VisSelectBox<String>()

    //This is so FUCKING dumb
    private var strArray = ArrayList<String>()
    private var listAdapter = ListAdapter(strArray)
    private var skinSelectionList = ListView(listAdapter)

    private var idLabel = VisLabel("Skin ID: ")
    private var statTrakLabel = VisLabel("StatTrak: ")
    private var wearLabel = VisLabel("Wear: ")

    private var skinIDInput = VisValidatableTextField(Validators.INTEGERS)
    private var skinStatTrak = VisValidatableTextField(Validators.INTEGERS)
    private var skinWear = VisSlider(0.0F, 1.0F, .01F, false)
    //private var skinWear = VisValidatableTextField(Validators.FLOATS)

    private var forceUpdate = VisTextButton("Manual Force Update")
    private var autoForceUpdate = VisCheckBoxCustom("Auto Force Update", "FORCE_UPDATE_AUTO")

    private var weaponSelected = "DESERT_EAGLE"
    private var minValue = 0.0F
    private var maxValue = 1.0F

    init {
        skinSelectionList.updatePolicy = ListView.UpdatePolicy.ON_DRAW
        val tmpStartArray = getSkinArray(weaponSelected)
        for (i in 0 until tmpStartArray.size) {
            listAdapter.add(tmpStartArray[i])
        }

        categorySelectionBox.setItems("PISTOL", "RIFLE", "SMG", "SNIPER", "SHOTGUN")
        categorySelectionBox.selected = "PISTOL"

        categorySelectionBox.changed { _, _ ->
            when (categorySelectionBox.selected) {
                "PISTOL" -> { weaponSelectionBox.clearItems(); weaponSelectionBox.setItems("DESERT_EAGLE", "DUAL_BERRETA", "FIVE_SEVEN", "GLOCK", "USP_SILENCER", "CZ75A", "R8_REVOLVER", "P2000", "TEC9", "P250") }
                "SMG" -> { weaponSelectionBox.clearItems(); weaponSelectionBox.setItems("MAC10", "P90", "MP5", "UMP45", "MP7", "MP9", "PP_BIZON") }
                "RIFLE" -> { weaponSelectionBox.clearItems(); weaponSelectionBox.setItems("AK47", "AUG", "FAMAS", "SG553", "GALIL", "M4A4", "M4A1_SILENCER", "NEGEV", "M249") }
                "SNIPER" -> { weaponSelectionBox.clearItems(); weaponSelectionBox.setItems("AWP", "G3SG1", "SCAR20", "SSG08") }
                "SHOTGUN" -> { weaponSelectionBox.clearItems(); weaponSelectionBox.setItems("XM1014", "MAG7", "SAWED_OFF", "NOVA") }
            }

            if (!weaponSelectionBox.items[0].isNullOrEmpty()) {
                weaponSelectionBox.selected = weaponSelectionBox.items[0]
            }
            true
        }

        weaponSelectionBox.setItems("DESERT_EAGLE", "DUAL_BERRETA", "FIVE_SEVEN", "GLOCK", "USP_SILENCER", "CZ75A", "R8_REVOLVER", "P2000", "TEC9", "P250")
        weaponSelectionBox.selected = "DESERT_EAGLE"
        weaponSelectionBox.changed { _, _ ->
            if (!weaponSelectionBox.selected.isNullOrEmpty()) {
                weaponSelected = weaponSelectionBox.selected

                val skinWep = curSettings["SKIN_$weaponSelected"].toSkinWeaponClass()

                skinIDInput.text = skinWep.tSkinID.toString()
                skinStatTrak.text = skinWep.tStatTrak.toString()
                skinWear.value = skinWep.tWear

                listAdapter.clear()
                val tmpArray = getSkinArray(weaponSelected)
                for (i in 0 until tmpArray.size) {
                    listAdapter.add(tmpArray[i])
                }
            }
        }

        skinSelectionList.setItemClickListener { str ->
            if (!str.isNullOrEmpty()) {
                val skinWep = curSettings["SKIN_$weaponSelected"].toSkinWeaponClass()
                skinIDInput.text = getSkinIDFromName(str, weaponSelected).toString()
                //Dont need to edit stat trak value
                minValue = getMinValueFromID(skinIDInput.text.toInt())
                maxValue = getMaxValueFromID(skinIDInput.text.toInt())

                skinWep.tSkinID = skinIDInput.text.toInt()
                if (skinWep.tWear < minValue) {
                    skinWep.tWear = minValue
                    skinWear.value = minValue
                    wearLabel.setText(minValue.toString())
                }
                if (skinWep.tWear > maxValue) {
                    skinWep.tWear = maxValue
                    skinWear.value = maxValue
                    wearLabel.setText(maxValue.toString())
                }

                curSettings["SKIN_$weaponSelected"] = skinWep.toString()
            }
        }

        knifeSelectionBox.setItems("Bayonet", "Classic", "Flip", "Gut", "Karambit", "M9 Bayonet", "Huntsman", "Falchion", "Bowie", "Butterfly", "Shadow Daggers", "Paracord", "Survival", "Ursus", "Navaja", "Nomad", "Stiletto", "Talon", "Skeleton")
        knifeSelectionBox.changed { _, _ ->
            curSettings["KNIFE_IDX"] = knifeSelectionBox.selectedIndex
            true
        }

        skinIDInput.changed { _, _ ->
            val skinWep = curSettings["SKIN_$weaponSelected"].toSkinWeaponClass()
            if (skinIDInput.isInputValid) {
                skinWep.tSkinID = skinIDInput.text.toInt()
                curSettings["SKIN_$weaponSelected"] = skinWep.toString()
            }

            true
        }

        skinStatTrak.changed { _, _ ->
            val skinWep = curSettings["SKIN_$weaponSelected"].toSkinWeaponClass()
            if (skinStatTrak.isInputValid) {
                skinWep.tStatTrak = skinStatTrak.text.toInt()
                curSettings["SKIN_$weaponSelected"] = skinWep.toString()
            }
        }

        skinWear.changed { _, _ ->
            when {
                skinWear.value < minValue -> {
                    skinWear.value = minValue
                    wearLabel.setText("Wear: $minValue")
                }
                skinWear.value > maxValue -> {
                    skinWear.value = maxValue
                    wearLabel.setText("Wear: $maxValue")
                }
                else -> {
                    wearLabel.setText("Wear: " + skinWear.value.toString())
                }
            }

            val skinWep = curSettings["SKIN_$weaponSelected"].toSkinWeaponClass()
            skinWep.tWear = skinWear.value
            curSettings["SKIN_$weaponSelected"] = skinWep.toString()
            true
        }

        enableSkinChanger.changed { _, _ ->
            skinChanger()
        }

        forceUpdate.changed { _, _ ->
            forcedUpdate()

            true
        }
        ////////////////////FORMATTING
        table.padLeft(25F)
        table.padRight(25F)

        val leftTable = VisTable(true)

        table.add(warningLabel).colspan(2).row()
        leftTable.add(enableSkinChanger).left().colspan(1).row()
        leftTable.add(categorySelectionBox).left().colspan(1).row()
        leftTable.add(weaponSelectionBox).left().colspan(1).row()
        //table.add(skinSelectionBox).colspan(2).row()

        leftTable.add(idLabel).left().row()
        leftTable.add(skinIDInput).left().row()
        leftTable.add(statTrakLabel).left().row()
        leftTable.add(skinStatTrak).left().row()
        leftTable.add(wearLabel).left().row()
        leftTable.add(skinWear).left().row()

        leftTable.add(forceUpdate).left().row()
        leftTable.add(autoForceUpdate).left().row()
        leftTable.add(enableKnifeChanger).left().row()
        leftTable.add(knifeSelectionBox).left().row()

        table.add(leftTable).width(220F).align(Align.topLeft)
        table.add(skinSelectionList.mainTable).right().colspan(1).width(220F)

        ////////////////////FORMATTING
    }

    override fun getContentTable(): Table? {
        return table
    }

    override fun getTabTitle(): String? {
        return "Skin Changer"
    }
}

//Youre fucking dumb as shit fuck off I'm retarded
private class ListAdapter(array: ArrayList<String>?) : ArrayListAdapter<String, VisTable>(array) {
    private var drawBG = VisUI.getSkin().getDrawable("window-bg")
    private var drawSelection = VisUI.getSkin().getDrawable("list-selection")

    init {
        selectionMode = SelectionMode.SINGLE
    }

    override fun createView(item: String): VisTable? {
        var label = VisLabel(item)
        label.setColor(1F, 1F, 1F, 1F)

        var table = VisTable()
        table.left()
        table.add(label)
        return table
    }

    override fun selectView(view: VisTable?) {
        view?.background = drawSelection
    }

    override fun deselectView(view: VisTable?) {
        view?.background = drawBG
    }
}


fun getSkinArray(wep: String): Array<String> {
    val wepSkinArray = Array<String>()
    var readingLines = false

    File("$SETTINGS_DIRECTORY\\SkinInfo\\SkinInfo.txt").forEachLine { line->
        if (readingLines) {
            if (line.startsWith("}")) {
                readingLines = false
            } else if (!line.startsWith("{")) { //Store the bois
                val tmpSplitLine = line.split(":")
                wepSkinArray.add(tmpSplitLine[0].trim()) //Add name
            }
        }

        if (line.startsWith(wep)) {
            readingLines = true
        }
    }

    return wepSkinArray
}

fun getSkinNameFromID(ID: Int): String {
    var str = ""
    var found = false
    File("$SETTINGS_DIRECTORY\\SkinInfo\\SkinInfo.txt").forEachLine { line->
        if (!found) {
            if (line.contains(":")) {
                val tmpSplitLine = line.split(":")
                if (tmpSplitLine[1].trim().toInt() == ID) {
                    str = tmpSplitLine[0].trim()
                    found = true
                }
            }
        }
    }

    return str
}

fun getSkinIDFromName(Name: String, Weapon: String): Int {
    var id = 0
    var inCategory = ""
    var found = false
    File("$SETTINGS_DIRECTORY\\SkinInfo\\SkinInfo.txt").forEachLine { line->
        if (!found) {
            if (line.contains("{")) {
                inCategory = line.replace("{", "").trim()
            }

            if (line.contains(":")) {
                val tmpSplitLine = line.split(":")
                if (tmpSplitLine[0].trim() == Name && inCategory == Weapon) {
                    id = tmpSplitLine[1].trim().toInt()
                    found = true
                }
            }
        }
    }

    return id
}

fun getMinValueFromID(ID: Int): Float {
    var minValue = 0.0F
    var found = false
    File("$SETTINGS_DIRECTORY\\SkinInfo\\SkinInfo.txt").forEachLine { line->
        if (!found) {
            if (line.contains(":")) {
                val tmpSplitLine = line.split(":")
                if (tmpSplitLine[1].trim().toInt() == ID) {
                    minValue = tmpSplitLine[2].trim().toFloat()
                    found = true
                }
            }
        }
    }

    return minValue
}

fun getMaxValueFromID(ID: Int): Float {
    var minValue = 0.0F
    var found = false
    File("$SETTINGS_DIRECTORY\\SkinInfo\\SkinInfo.txt").forEachLine { line->
        if (!found) {
            if (line.contains(":")) {
                val tmpSplitLine = line.split(":")
                if (tmpSplitLine[1].trim().toInt() == ID) {
                    minValue = tmpSplitLine[3].trim().toFloat()
                    found = true
                }
            }
        }
    }

    return minValue
}

fun skinChangerTabUpdate() {

}