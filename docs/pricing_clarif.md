- **CLARIF-01:** As management, I can preset the **additional costs** (default or category-based) and the **markup/margin** rules that the system uses to calculate SRPs for each channel, ensuring these parameters are standardized and not alterable by assistants. I expect the following presets:
    **hauling weight**: 700kg  
    **driver Fee**: 2000  
    **fuel**: 4000  
    **toll**: 1000  
    **handling**: 200  
    **additional costs**: (**driver fee** + **fuel** + **toll** + **handling**) / **hauling weight**  

    channel markup: **online markup**: .35, **offline markup**: .30, **reseller markup**: .25  
    **spoilage**: 25% of weight of total_quantity of item acquired or can be customized to actual weight, e.g. 2kg  
    
    For items which are to be sold by weight:
    **SRP per kg per channel:**
        **sellable kg after spoilage** *(management chooses one approach — see **`docs/PricingReference.md`** §4.3 / §4.3.1, **BUG-PRC-04** for app implementation):*
            — **Rate:** **total_quantity** × (1 − **spoilage**), where **spoilage** is a **fraction** of acquired weight (e.g. **25%** of **total_quantity**); **or**
            — **Actual mass:** **total_quantity** − **spoilage** when **spoilage** is recorded as **kilograms** not sellable (e.g. **2 kg**), with sellable kg **> 0**
        **A**: **bulk purchase cost** / **sellable kg after spoilage**
        **SRP**: (**A** + **additional costs**) x (1 + **channel markup**)

    For items which are to be sold per piece:
    **SRP per piece per channel:**
        **A**: **bulk purchase cost** / **total_quantity** (cost per piece)  
        **Estimated Qty per Kg**: estimated quantity per kg from user input  
        **B**: (**total_quantity** / **Estimated Qty per Kg**) × **additional costs** (= derived **kg** × **additional costs** per kg = **total hauling for the lot** in PHP)
        **SRP**: (**A** + **B** / **total_quantity**) x (1 + **channel markup**) (per piece — **B** is lot total; hauling share per piece is **B** / **total_quantity** = **additional costs** / **Estimated Qty per Kg**)
        **spoilage**: for item sold per piece, is 0. Is not part of the SRP calculations

    **Categories**: Vegetables, Fruits, Other Dry Goods  
