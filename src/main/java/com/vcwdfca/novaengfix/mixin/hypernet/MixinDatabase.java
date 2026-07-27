package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.Database;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchCognitionData;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mixin(value = Database.class, remap = false)
public abstract class MixinDatabase {
    @Shadow(remap = false)
    @Final
    private Set<ResearchCognitionData> storedResearchCognition;

    @Shadow(remap = false)
    @Final
    private Object2DoubleOpenHashMap<ResearchCognitionData> researchingCognition;

    @Unique
    private final Set<String> novaengfix$unresolvedStoredResearch = new LinkedHashSet<>();

    @Unique
    private final Map<String, Double> novaengfix$unresolvedResearching = new HashMap<>();

    @Unique
    private final Set<String> novaengfix$warnedResearchNames = new HashSet<>();

    @Unique
    private TileMultiblockMachineController novaengfix$getOwner() {
        return ((Database) (Object) this).getOwner();
    }

    @Inject(method = "readNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), remap = false)
    private void novaengfix$rememberUnresolvedResearch(NBTTagCompound customData, CallbackInfo ci) {
        novaengfix$unresolvedStoredResearch.clear();
        novaengfix$unresolvedResearching.clear();

        if (customData.hasKey("storedResearchCognition")) {
            NBTTagList stored = customData.getTagList("storedResearchCognition", Constants.NBT.TAG_STRING);
            for (int i = 0; i < stored.tagCount(); i++) {
                String name = stored.getStringTagAt(i);
                if (RegistryHyperNet.getResearchCognitionData(name) == null) {
                    novaengfix$unresolvedStoredResearch.add(name);
                    novaengfix$warnUnknown(name);
                }
            }
        }

        if (customData.hasKey("researchingCognition")) {
            NBTTagList researching = customData.getTagList("researchingCognition", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < researching.tagCount(); i++) {
                NBTTagCompound entry = researching.getCompoundTagAt(i);
                String name = entry.getString("researchName");
                if (RegistryHyperNet.getResearchCognitionData(name) == null) {
                    novaengfix$unresolvedResearching.put(name, entry.getDouble("progress"));
                    novaengfix$warnUnknown(name);
                }
            }
        }
    }

    @Inject(method = "writeNBT()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$pauseWriteDuringReload(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
            return;
        }
        if (!HyperNetFixLifecycle.isServerThread(novaengfix$getOwner())) {
            HyperNetFixLifecycle.scheduleNodeWrite((github.kasuminova.novaeng.common.hypernet.old.NetNode) (Object) this);
            ci.cancel();
            return;
        }
        novaengfix$resolveAvailableResearch();
    }

    @Inject(method = "writeNBT()V", at = @At("RETURN"), remap = false)
    private void novaengfix$mergeUnresolvedResearch(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            return;
        }

        NBTTagCompound tag = novaengfix$getOwner().getCustomDataTag();
        if (!novaengfix$unresolvedStoredResearch.isEmpty()) {
            NBTTagList stored = tag.getTagList("storedResearchCognition", Constants.NBT.TAG_STRING);
            Set<String> present = new HashSet<>();
            for (int i = 0; i < stored.tagCount(); i++) {
                present.add(stored.getStringTagAt(i));
            }
            for (String name : novaengfix$unresolvedStoredResearch) {
                if (present.add(name)) {
                    stored.appendTag(new net.minecraft.nbt.NBTTagString(name));
                }
            }
            tag.setTag("storedResearchCognition", stored);
        }

        if (!novaengfix$unresolvedResearching.isEmpty()) {
            NBTTagList researching = tag.getTagList("researchingCognition", Constants.NBT.TAG_COMPOUND);
            Set<String> present = new HashSet<>();
            for (int i = 0; i < researching.tagCount(); i++) {
                present.add(researching.getCompoundTagAt(i).getString("researchName"));
            }
            for (Map.Entry<String, Double> entry : novaengfix$unresolvedResearching.entrySet()) {
                if (present.add(entry.getKey())) {
                    NBTTagCompound research = new NBTTagCompound();
                    research.setString("researchName", entry.getKey());
                    research.setDouble("progress", entry.getValue());
                    researching.appendTag(research);
                }
            }
            tag.setTag("researchingCognition", researching);
        }

        HyperNetFixLifecycle.markDirty(novaengfix$getOwner());
    }

    @Unique
    private void novaengfix$resolveAvailableResearch() {
        for (Iterator<String> iterator = novaengfix$unresolvedStoredResearch.iterator(); iterator.hasNext();) {
            ResearchCognitionData data = RegistryHyperNet.getResearchCognitionData(iterator.next());
            if (data != null) {
                storedResearchCognition.add(data);
                iterator.remove();
            }
        }

        for (Iterator<Map.Entry<String, Double>> iterator = novaengfix$unresolvedResearching.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Double> entry = iterator.next();
            ResearchCognitionData data = RegistryHyperNet.getResearchCognitionData(entry.getKey());
            if (data != null) {
                researchingCognition.put(data, entry.getValue().doubleValue());
                iterator.remove();
            }
        }
    }

    @Unique
    private void novaengfix$warnUnknown(String name) {
        if (novaengfix$warnedResearchNames.add(name)) {
            github.kasuminova.novaeng.NovaEngineeringCore.log.warn(
                "Keeping unresolved HyperNet research '{}' in database NBT.", name
            );
        }
    }
}
