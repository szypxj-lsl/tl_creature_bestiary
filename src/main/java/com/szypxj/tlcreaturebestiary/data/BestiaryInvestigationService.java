package com.szypxj.tlcreaturebestiary.data;

import com.szypxj.tlcreaturebestiary.config.TCBConfig;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.api.progress.PlayerProgressApi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class BestiaryInvestigationService {
    public static final int MAX_SPECIES_LENGTH = 128;
    public static final int MAX_DESCRIPTION_LENGTH = 4096;
    private BestiaryInvestigationService() {}
    public static boolean isHighRisk(LivingEntity target){ return BestiaryInvestigationRules.isHighRisk(target); }
    public static boolean isHighRisk(EntityType<?> type){ return BestiaryInvestigationRules.isHighRisk(type); }
    public static boolean isHighRisk(ResourceLocation entityTypeId){ return BestiaryInvestigationRules.isHighRisk(BestiaryCanonicalization.canonical(entityTypeId)); }
    public static int baseDangerStars(LivingEntity target){ return target==null?CreatureInfoApi.DANGER_MIN_STARS:BestiaryInvestigationRules.baseDangerStars(target.getType()); }
    public static BestiaryEntryProgress progress(ServerPlayer player, EntityType<?> type){ ResourceLocation id=type==null?null:BestiaryCanonicalization.canonical(ForgeRegistries.ENTITY_TYPES.getKey(type)); if(player==null||id==null)return BestiaryEntryProgress.empty(); migrateLegacyIfNeeded(player,type,id); return BestiaryData.progress(player,id); }
    public static boolean recordObservation(ServerPlayer player, LivingEntity target, ResourceLocation entityTypeId){
        if(player==null||target==null||entityTypeId==null||!isHighRisk(target))return false;
        ResourceLocation targetId=BestiaryCanonicalization.canonical(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
        entityTypeId=BestiaryCanonicalization.canonical(entityTypeId);
        if(targetId==null||!entityTypeId.equals(targetId))return false;
        migrateLegacyIfNeeded(player,target.getType(),entityTypeId);
        BestiaryEntryProgress current=BestiaryData.progress(player,entityTypeId), updated; String promptKey;
        if(!current.observed()){ updated=current.withObserved(true); promptKey="msg.tl_creature_bestiary.investigation.first_observation"; }
        else if(current.combatRecorded()&&!current.tamingRecorded()){ updated=current.withTamingRecorded(true); promptKey="msg.tl_creature_bestiary.investigation.second_observation"; }
        else return false;
        ensureVisible(player,entityTypeId); persistAndFinalize(player,entityTypeId,updated); player.displayClientMessage(Component.translatable(promptKey),true); return true;
    }
    public static boolean recordCombat(ServerPlayer player, LivingEntity target){
        if(player==null||target==null||!isHighRisk(target))return false;
        ResourceLocation id=BestiaryCanonicalization.canonical(ForgeRegistries.ENTITY_TYPES.getKey(target.getType())); if(id==null)return false;
        migrateLegacyIfNeeded(player,target.getType(),id); BestiaryEntryProgress current=BestiaryData.progress(player,id); if(current.combatRecorded())return false; persistAndFinalize(player,id,current.withCombatRecorded(true)); return true;
    }
    public static boolean savePlayerNotes(ServerPlayer player,ResourceLocation id,String species,String description){
        if(player==null||id==null||!isHighRisk(id))return false; id=BestiaryCanonicalization.canonical(id); if(!BestiaryData.isUnlocked(player,id))return false;
        if(!isValidEditableText(species,MAX_SPECIES_LENGTH,false)||!isValidEditableText(description,MAX_DESCRIPTION_LENGTH,true))return false;
        EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(id); if(type==null)return false; migrateLegacyIfNeeded(player,type,id);
        BestiaryEntryProgress updated=BestiaryData.progress(player,id).withPlayerNotes(species,description); BestiaryData.setProgress(player,id,updated); finalizeCompletionReward(player,id,updated); BestiaryNetwork.sendProgress(player,id,BestiaryData.progress(player,id),false); return true;
    }
    public static boolean claimCompletionReward(ServerPlayer player,ResourceLocation id){
        if(player==null||id==null||!isHighRisk(id))return false; id=BestiaryCanonicalization.canonical(id); EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(id); if(type==null)return false; migrateLegacyIfNeeded(player,type,id); boolean claimed=finalizeCompletionReward(player,id,BestiaryData.progress(player,id)); if(claimed)BestiaryNetwork.sendProgress(player,id,BestiaryData.progress(player,id)); return claimed;
    }
    public static void migrateLegacyIfNeeded(ServerPlayer player,EntityType<?> type,ResourceLocation id){ if(player==null||type==null||id==null||!isHighRisk(type))return; if(BestiaryData.isUnlocked(player,id)&&!BestiaryData.hasProgress(player,id))BestiaryData.setProgress(player,id,BestiaryEntryProgress.empty().withObserved(true)); }
    private static void persistAndFinalize(ServerPlayer player,ResourceLocation id,BestiaryEntryProgress updated){ BestiaryData.setProgress(player,id,updated); finalizeCompletionReward(player,id,updated); BestiaryNetwork.sendProgress(player,id,BestiaryData.progress(player,id)); }
    private static boolean finalizeCompletionReward(ServerPlayer player,ResourceLocation id,BestiaryEntryProgress current){
        if(player==null||id==null||current==null||!current.completed()||current.rewardClaimed())return false; int reward=TCBConfig.HIGH_RISK_COMPLETION_REWARD_POINTS.get();
        if(reward<=0){ BestiaryData.setProgress(player,id,current.withRewardClaimed(true)); player.sendSystemMessage(Component.translatable("msg.tl_creature_bestiary.investigation.completed")); return true; }
        if(!PlayerProgressApi.addUnspentAttributePoints(player,reward))return false; BestiaryData.setProgress(player,id,current.withRewardClaimed(true)); player.sendSystemMessage(Component.translatable("msg.tl_creature_bestiary.investigation.completed_reward",reward)); return true;
    }
    private static boolean isValidEditableText(String value,int maxLength,boolean multiline){ if(value==null||value.length()>maxLength)return false; for(int i=0;i<value.length();i++){ char c=value.charAt(i); if(!Character.isISOControl(c))continue; if(c=='\t'||(multiline&&(c=='\n'||c=='\r')))continue; return false;} return true; }
    private static void ensureVisible(ServerPlayer player,ResourceLocation id){ if(BestiaryData.unlock(player,id))BestiaryNetwork.sendUnlock(player,id); }
}
