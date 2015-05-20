/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.api.preset;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.NotImplementedException;

import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.ctrl.Controllable;
import cn.academy.core.registry.RegDataPart;
import cn.academy.core.util.DataPart;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("preset")
public class PresetData extends DataPart {
	
	public static final int MAX_KEYS = 8, MAX_PRESETS = 4;
	
	int presetID = 0;
	Preset[] presets = new Preset[4];

	//Hooks that are related to SpecialSkill
	// TODO: Implement
	boolean locked = false;
	Preset specialPreset;
	
	/*
	 * Notify: Unlike normal DataParts, PresetData is
	 * client-major after the initial creation.
	 */
	
	public PresetData() {
		for(int i = 0; i < MAX_PRESETS; ++i) {
			presets[i] = new Preset();
		}
	}
	
	private AbilityData getAbilityData() {
		return AbilityData.get(getPlayer());
	}
	
	public void override(Preset special) {
		throw new NotImplementedException("override");
	}
	
	public void endOverride() {
		throw new NotImplementedException("endOverride");
	}
	
	/**
	 * Client only. Create a instance that have capability to edit a fixed preset.
	 */
	public PresetEditor createEditor(int presetID) {
		return new PresetEditor(presets[presetID]);
	}
	
	public Preset getCurrentPreset() {
		if(!getIsActive()) {
			return null;
		}
		return locked ? specialPreset : presets[presetID];
	}
	
	@Override
	public void tick() {}
	
	private void syncToServer() {
		recServerSync(toNBT());
	}
	
	@RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
	private void recServerSync(@Data NBTTagCompound tag) {
		fromNBT(tag);
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		presetID = tag.getByte("cur");
		for(int i = 0; i < MAX_PRESETS; ++i) {
			presets[i].fromNBT(tag.getCompoundTag("" + i));
		}
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound ret = new NBTTagCompound();
		ret.setByte("cur", (byte) presetID);
		for(int i = 0; i < MAX_PRESETS; ++i) {
			ret.setTag("" + i, presets[i].toNBT());
		}
		return ret;
	}
	
	private boolean getIsActive() {
		if(isRemote()) {
			return this.isSynced() && getAbilityData().isLearned();
		}
		return getAbilityData().isLearned();
	}
	
	public class PresetEditor {
		
		final byte display[] = new byte[MAX_KEYS];
		
		private final Preset target;
		
		public PresetEditor(Preset _target) {
			target = _target;
		}
		
		public void edit(int key, int newMapping) {
			display[key] = (byte) newMapping;
		}
		
		public boolean hasChanged() {
			for(int i = 0; i < MAX_KEYS; ++i) {
				if(display[i] != target.data[i])
					return true;
			}
			return false;
		}
		
		public void save() {
			target.setData(display);
			syncToServer();
		}
		
	}
	
	public class Preset {
		
		final byte data[] = new byte[MAX_KEYS];
		
		public Preset() {
			for(int i = 0; i < MAX_KEYS; ++i) {
				data[i] = -1;
			}
		}
		
		public Preset(byte[] _data) {
			setData(_data);
		}
		
		void setData(byte[] _data) {
			for(int i = 0; i < MAX_KEYS; ++i) {
				data[i] = _data[i];
			}
		}
		
		public Controllable getControllable(int key) {
			int mapping = data[key];
			if(mapping == -1) {
				return null;
			}
			AbilityData data = getAbilityData();
			Category cat = data.getCategory();
			if(cat == null) return null;
			return cat.getControllable(mapping);
		}
		
		NBTTagCompound toNBT() {
			NBTTagCompound ret = new NBTTagCompound();
			ret.setByteArray("l", data);
			return ret;
		}
		
		void fromNBT(NBTTagCompound tag) {
			byte[] d = tag.getByteArray("l");
			for(int i = 0; i < MAX_KEYS; ++i) {
				data[i] = d[i];
			}
		}
		
	}

}
