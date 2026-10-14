package com.qince.starminer.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Data generators for StarMiner. Run with {@code gradlew runData}. */
public final class SMDataGenerators {

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(), new EnglishLanguage(output));
        generator.addProvider(event.includeClient(), new ChineseLanguage(output));
    }

    /** {@code assets/starminer/lang/en_us.json} */
    public static class EnglishLanguage extends LanguageProvider {
        public EnglishLanguage(PackOutput output) {
            super(output, "starminer", "en_us");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup.starminer", "Starminer");

            add("block.starminer.gravitycore", "Gravity Core");
            add("item.starminer.starcore_dust", "Stardust");
            add("item.starminer.earthstone", "Earth Stone");

            add("starInfo.titleInf", "Star Info");
            add("starInfo.titleGrange", "G-Rad");
            add("starInfo.titleSSize", "StarRad");
            add("starInfo.titleType", "Shape");
            add("starInfo.gRangeInfo", ">>> %d <<<");
            add("starInfo.gSizeInfo", ">>> %d <<<");
            add("starInfo.type.sph", "Sphere");
            add("starInfo.type.cub", "Cube");
            add("starInfo.type.xcyl", "X Cylinder");
            add("starInfo.type.ycyl", "Y Cylinder");
            add("starInfo.type.zcyl", "Z Cylinder");
            add("starInfo.sconInfoON", "coord[x:%d,y:%d,z:%d]");
            add("starInfo.sconInfoOFF", "not active");
            add("starInfo.creativeonly", "Creative mode only");
            add("starInfo.bufferzone", "Buffer Zone");
            add("starInfo.working", "Terraforming...");
            add("starInfo.idle", "Idle");
            add("starInfo.progress", "Blocks: %s / %s");
            add("starInfo.titleSpeed", "Build Speed");
            add("starInfo.short.grange", "G %s");
            add("starInfo.short.starrad", "R %s");

            add("starInfo.speed.slow", "Very Slow");
            add("starInfo.speed.normal", "Normal");
            add("starInfo.speed.fast", "Fast");

            add("starInfo.ctl.hint", "Left click: +1 | Right click: -1 | Hold Shift x10 | Hold Ctrl x100");
            add("starInfo.ctl.gravity_range.plus", "+");
            add("starInfo.ctl.gravity_range.minus", "-");
            add("starInfo.ctl.star_radius.plus", "+");
            add("starInfo.ctl.star_radius.minus", "-");
            add("starInfo.ctl.field_type.cycle", "T");
            add("starInfo.ctl.build_speed.cycle", "S");
            add("starInfo.ctl.buffer.cycle", "B");

            add("starInfo.gdir", "Gravity: %s");
            add("starInfo.gdir.UP_TO_DOWN_Y_N", "Down");
            add("starInfo.gdir.DOWN_TO_UP_Y_P", "Up (inverted)");
            add("starInfo.gdir.EAST_TO_WEST_X_N", "West");
            add("starInfo.gdir.WEST_TO_EAST_X_P", "East");
            add("starInfo.gdir.NORTH_TO_SOUTH_Z_P", "South");
            add("starInfo.gdir.SOUTH_TO_NORTH_Z_N", "North");

            add("starminer.configuration.maxGravityRange", "Max Gravity Range");
            add("starminer.configuration.maxStarRadius", "Max Star Radius");
            add("starminer.configuration.terraforming.blocksPerCycle", "Blocks Per Work Cycle");
            add("starminer.configuration.terraforming.cycleTicks", "Ticks Per Work Cycle");
            add("starminer.configuration.gravity.applyAttribute", "Apply Gravity Attribute");
            add("starminer.configuration.gravity.flippedMultiplier", "Flipped Gravity Multiplier");
            add("starminer.configuration.gravity.sidewaysMultiplier", "Sideways Gravity Multiplier");
        }
    }

    /** {@code assets/starminer/lang/zh_cn.json}, translated from the original mod's {@code zh_CN.lang}. */
    public static class ChineseLanguage extends LanguageProvider {
        public ChineseLanguage(PackOutput output) {
            super(output, "starminer", "zh_cn");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup.starminer", "星际矿工");

            add("block.starminer.gravitycore", "重力核心");
            add("item.starminer.starcore_dust", "星屑");
            add("item.starminer.earthstone", "地球石");

            add("starInfo.titleInf", "行星信息");
            add("starInfo.titleGrange", "引力范围");
            add("starInfo.titleSSize", "行星尺寸");
            add("starInfo.titleType", "形态");
            add("starInfo.gRangeInfo", ">>> %d <<<");
            add("starInfo.gSizeInfo", ">>> %d <<<");
            add("starInfo.type.sph", "球形");
            add("starInfo.type.cub", "方形");
            add("starInfo.type.xcyl", "X轴圆桶");
            add("starInfo.type.ycyl", "Y轴圆筒");
            add("starInfo.type.zcyl", "Z轴圆筒");
            add("starInfo.sconInfoON", "坐标[x:%d,y:%d,z:%d]");
            add("starInfo.sconInfoOFF", "没有坐标");
            add("starInfo.creativeonly", "创造模式限定物品");
            add("starInfo.bufferzone", "缓冲层");
            add("starInfo.working", "正在改造...");
            add("starInfo.idle", "空闲");
            add("starInfo.progress", "方块: %s / %s");
            add("starInfo.titleSpeed", "建造速度");
            add("starInfo.short.grange", "引 %s");
            add("starInfo.short.starrad", "尺 %s");

            add("starInfo.speed.slow", "很慢");
            add("starInfo.speed.normal", "普通");
            add("starInfo.speed.fast", "快速");

            add("starInfo.ctl.hint", "左键 +1 | 右键 -1 | 按住 Shift 十倍 | 按住 Ctrl 百倍");
            add("starInfo.ctl.gravity_range.plus", "+");
            add("starInfo.ctl.gravity_range.minus", "-");
            add("starInfo.ctl.star_radius.plus", "+");
            add("starInfo.ctl.star_radius.minus", "-");
            add("starInfo.ctl.field_type.cycle", "形");
            add("starInfo.ctl.build_speed.cycle", "速");
            add("starInfo.ctl.buffer.cycle", "缓");

            add("starInfo.gdir", "引力方向: %s");
            add("starInfo.gdir.UP_TO_DOWN_Y_N", "向下");
            add("starInfo.gdir.DOWN_TO_UP_Y_P", "向上(倒挂)");
            add("starInfo.gdir.EAST_TO_WEST_X_N", "向西");
            add("starInfo.gdir.WEST_TO_EAST_X_P", "向东");
            add("starInfo.gdir.NORTH_TO_SOUTH_Z_P", "向南");
            add("starInfo.gdir.SOUTH_TO_NORTH_Z_N", "向北");

            add("starminer.configuration.maxGravityRange", "最大引力范围");
            add("starminer.configuration.maxStarRadius", "最大行星尺寸");
            add("starminer.configuration.terraforming.blocksPerCycle", "每周期放置方块数");
            add("starminer.configuration.terraforming.cycleTicks", "每周期耗时(tick)");
            add("starminer.configuration.gravity.applyAttribute", "应用重力属性");
            add("starminer.configuration.gravity.flippedMultiplier", "翻转重力倍率");
            add("starminer.configuration.gravity.sidewaysMultiplier", "侧向重力倍率");
        }
    }

    private SMDataGenerators() {
    }
}
