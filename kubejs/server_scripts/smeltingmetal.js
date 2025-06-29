// kubejs/server_scripts/smelting_overhaul.js

// This event is fired when recipes are loaded or reloaded (e.g., after /reload command)
ServerEvents.recipes(event => {
    const MOLTEN_METAL_ITEM_ID = 'smeltingmetal:molten_metal';
    function createMoltenMetal(metalType) {
        return Item.of(MOLTEN_METAL_ITEM_ID, `{MetalType:"${metalType}"}`);
    }
    const commonIngotTags = [
        'forge:ingots', // Standard Forge ingot tag (e.g., forge:ingots/iron, forge:ingots/copper)
        'c:ingots'      // Common convention for ingots (often used in Fabric/Quilt, but also by Forge mods for compatibility)
        // You can add more specific tags here if you know a particular mod uses a unique tag for its ingots, e.g.:
        // 'some_mod_id:special_ingots'
    ];

    const commonOreTags = [
        'forge:ores',   // Standard Forge ore tag (e.g., forge:ores/iron, forge:ores/copper)
        'c:ores'        // Common convention for ores
        // Add more if needed, e.g., 'some_mod_id:deepslate_ores'
    ];

    // --- Dynamic Recipe Processing ---
    const smeltingAndBlastingRecipes = event.getAllRecipes().filter(r =>
        r.type === 'minecraft:smelting' ||  // Vanilla furnace smelting
        r.type === 'minecraft:blasting'     // Vanilla blast furnace smelting
    );

    // Iterate over each detected smelting/blasting recipe
    smeltingAndBlastingRecipes.forEach(recipe => {
        const outputItem = recipe.output; // The item produced by the original recipe
        const inputItem = recipe.input;   // The item consumed by the original recipe (can be an item or a tag)

        let isMetalIngotOutput = false;
        let metalType = null; // This will store 'iron', 'gold', 'copper', 'steel', etc.

        // --- Determine if the Output is an Ingot ---

        // 1. Check if the output item has any of the common ingot tags
        for (const tag of commonIngotTags) {
            if (outputItem.hasTag(tag)) {
                isMetalIngotOutput = true;
                // Extract the metal type from the tag (e.g., 'forge:ingots/iron' -> 'iron')
                const fullTagName = outputItem.getTags().find(t => t.startsWith(tag));
                if (fullTagName) {
                    const parts = fullTagName.split('/');
                    metalType = parts[parts.length - 1]; // Get the last part of the tag (e.g., 'iron')
                    break; // Stop checking tags once one is found
                }
            }
        }

        // 2. Fallback: Check if the output item's ID contains "_ingot"
        // This catches ingots that might not be perfectly tagged but follow a naming convention.
        if (!isMetalIngotOutput && outputItem.id.includes('_ingot')) {
            isMetalIngotOutput = true;
            const idParts = outputItem.id.split(':'); // Split by modid:itemid (e.g., ['minecraft', 'iron_ingot'])
            if (idParts.length > 1) {
                const nameParts = idParts[1].split('_'); // Split itemid by underscore (e.g., ['iron', 'ingot'])
                if (nameParts.length > 1 && nameParts[nameParts.length - 1] === 'ingot') {
                    // Rejoin in case of multi-word metal names like 'dark_iron_ingot' -> 'dark_iron'
                    metalType = nameParts.slice(0, -1).join('_');
                }
            }
        }

        // --- If it's a metal ingot, replace the recipe ---
        if (isMetalIngotOutput && metalType) {
            // Remove the original recipe to prevent it from ever being used.
            // Removing by recipe.id is the most specific and safest way.
            event.remove({ id: recipe.id });

            // Add your new recipe using the determined input and the custom molten metal blob.
            if (recipe.type === 'minecraft:smelting') {
                event.smelting(createMoltenMetal(metalType), inputItem);
                console.log(`[Smelting Metal Mod] Replaced smelting recipe: ${recipe.id} -> Molten ${metalType}`);
            } else if (recipe.type === 'minecraft:blasting') {
                event.blasting(createMoltenMetal(metalType), inputItem);
                console.log(`[Smelting Metal Mod] Replaced blasting recipe: ${recipe.id} -> Molten ${metalType}`);
            }
        }
    });

});