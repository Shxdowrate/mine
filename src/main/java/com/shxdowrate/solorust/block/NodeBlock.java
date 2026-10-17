package com.shxdowrate.solorust.block;

import net.minecraft.world.level.block.Block;

/**
 * Блок рудного валуна как в Расте. Валун состоит из нескольких таких блоков;
 * удар киркой по одному ломает весь валун (см. VeinMiner).
 */
public class NodeBlock extends Block {
	public NodeBlock(Properties properties) {
		super(properties);
	}
}
