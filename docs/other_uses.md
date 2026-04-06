# Other Business Adaptations

**Based on:** RedN Farm App — structure, design, user stories, and server architecture  
**Date:** 2026-04-06

This document identifies 15 business contexts where the core architecture of this app — offline-first multi-device POS, cost-to-SRP pricing engine, acquisition/procurement tracking, operations logging, employee payment management, and server-side consolidation — maps naturally to a real operational need.

Each entry describes the business, how the existing concepts translate, what adaptations the app would need, what specific value it delivers, and how it compares to existing solutions found on the Google Play Store and Apple App Store. App store research was conducted in April 2026.

---

## 1. Wet Market / Public Market Stall Network

**Business:** A market vendor or cooperative that operates multiple stalls inside a public market, buying produce in bulk from farm suppliers at dawn and retailing through the day.

### Concept mapping

| Farm App concept | Market stall equivalent |
|---|---|
| Acquisition | Pre-dawn bulk buy from truckers/wholesalers — price per kg recorded at the gate |
| Spoilage rate | Unsold / wilted produce written off daily — rate varies by product |
| SRP per channel | Stallside retail, reseller (suki / canteen buyers), and online pre-order for pickup |
| Farm operations | Cleaning, sorting, trimming, ice replenishment logs |
| Employees | Stall workers paid daily wages with cash advance |
| Active SRPs screen | Price board posted at the stall — printable on the Sunmi's thermal printer |
| Multi-device | One device per stall; server consolidates across all stalls in the cooperative |

### Key adaptations
- Replace `AcquisitionLocation` enum (FARM / MARKET / SUPPLIER) with stall-relevant source types: `WHOLESALER`, `DIRECT_FARMER`, `TRADERS_HUB`, `OWN_FARM`.
- Add a **"daily closing"** operation type: unsold quantity written off, confirmed spoilage entered, ending stock reconciled.
- Pricing preset cadence is daily rather than quarterly — the admin activates a new preset each morning based on the gate price.

### Specific value
Most market stall operators price from memory or a whiteboard. This app gives them a structured cost-in → SRP-out workflow, a printable price board, and a server-side record of every day's procurement cost and sales — useful for BIR compliance and supplier negotiation.

### Existing solutions
**Peddlr** (Android/iOS, free) is the closest — offline-capable with Bluetooth thermal printing and a customer credit ledger. **UTAK POS** (~PHP 2,333/month) and **SariStore POS** (free) also target this segment. Critical gap across all: none have a cost-to-SRP engine. A vendor who paid PHP 35/kg for tomatoes with 15% expected spoilage and PHP 8/kg hauling gets no help computing the correct selling price — they still guess.

---

## 2. Fish Port / Seafood Distribution Hub

**Business:** A fish landing port or distribution point that receives catch from fishing boats, sorts by species and grade, and sells to restaurants, retailers, and exporters.

### Concept mapping

| Farm App concept | Fish port equivalent |
|---|---|
| Acquisition | Unloading from boat — species, weight, price per kg from the vessel owner |
| `is_per_kg` / `piece_count` | Most fish sold per kg; live crabs/lobsters sold per piece with a per-kg equivalent |
| Spoilage rate | High and time-dependent — fish grade degrades within hours; spoilage rate differs by species |
| Channels | Restaurant direct, retail market, export buyer, online order (consumer delivery) |
| SRP computation | Cost per sellable kg (net of ice/shrink loss) + transport fees → channel markup |
| Farm operations | Ice loading, sorting, grading, cold storage temperature logs |
| Remittances | Payment to boat owners / fishing crews |
| Employees | Port workers — sorters, loaders, ice handlers |

### Key adaptations
- Add **species** and **grade** (A/B/C) as product attributes replacing `category`.
- Add **time-to-expiry** as a derived field from `date_acquired` — used to flag products approaching spoilage in the Active SRPs screen.
- `piece_count` field repurposed: for whole fish it becomes pieces per basket rather than pieces per kg.
- Operations log extended with cold chain entries: ice replenishment, temperature readings.

### Specific value
Fish ports handle enormous volumes with extremely perishable goods and multiple buyer classes arriving in waves. The pricing engine's spoilage-adjusted cost math is exactly the calculation a port manager does mentally at 4am — this app makes it auditable and repeatable across buyers.

### Existing solutions
**Markt POS** (US, custom pricing) has scale integration and shrink auditing but is US-centric, cloud-dependent, and hardware-terminal-based — not a mobile Android solution. **FoodReady** (~$100+/month) focuses on HACCP food safety, not port-side POS. The BFAR **FishR** government app targets aquaculture, not distribution. **No existing app** handles the port-buyer workflow: pre-dawn landing price entry + spoilage adjustment + channel pricing + offline thermal receipt printing for fish buyers.

---

## 3. Poultry and Livestock Farm

**Business:** A small-to-medium poultry farm (broilers, layers) or livestock operation (hogs, goats) that manages production cycles, feed acquisition, animal inventory, and sale of dressed or live animals.

### Concept mapping

| Farm App concept | Poultry / livestock equivalent |
|---|---|
| Acquisition | Feed purchase (per kg, per sack), chick/piglet purchase (per head = per piece) |
| `piece_count` | Chicks per delivery batch; feed bags per delivery |
| Farm operations | Feeding logs, vaccination schedules, mortality entries, weighing events |
| `FarmOperationType` | Expand to: `VACCINATION`, `WEIGHING`, `MORTALITY_ENTRY`, `CULLING`, `MEDICATION` |
| SRP computation | Feed cost per kg of live weight gained → dressed weight margin per channel |
| Channels | Farm-gate (live), slaughterhouse (dressed weight), supermarket (portioned) |
| Employees | Farm workers with daily wages and cash advance |
| Remittances | Payments to feed suppliers or veterinarians |

### Key adaptations
- Add **batch/flock tracking** — each acquisition of chicks or piglets starts a production cycle. Operations and final sale are linked to that batch.
- Replace per-acquisition SRP with **batch-level cost accumulation**: total feed cost + vet cost + labor across the batch's lifecycle feeds into a final dressed-weight SRP computation.
- Add **mortality rate** as an analog to spoilage rate — reduces effective sellable units.

### Specific value
Poultry and livestock farmers rarely track true cost-per-kg because feed costs, mortality, and labor all accumulate over 6–12 weeks before a sale. The pricing engine's cost-in → sellable unit math directly solves the batch profitability question they struggle with most.

### Existing solutions
**My Poultry Manager** (Freemium), **Easy Poultry & Chicken Manager** (Freemium), and **FlockFarm** all track feed costs and mortality logs. The universal gap: costs are logged but never fed into a pricing recommendation. None compute "given feed spend of PHP 28,000 and 94% survival rate on 500 chicks, what is the break-even price per kg live weight?" No thermal printing for farm-gate invoices to buyers or slaughterhouse receipts.

---

## 4. Home-Based / Cottage Food Production

**Business:** A home-based food producer making processed goods (atchara, pastillas, longganisa, dried fish, jams) that sells through multiple channels — online orders, local stores, and weekend markets.

### Concept mapping

| Farm App concept | Cottage food equivalent |
|---|---|
| Acquisition | Raw ingredient purchase: vinegar, sugar, containers, packaging |
| Farm operations | Production runs — batch number, yield, packaging count |
| `FarmOperationType` | `PRODUCTION_RUN`, `PACKAGING`, `QUALITY_CHECK`, `INVENTORY_COUNT` |
| SRP computation | Total ingredient cost per batch ÷ yield units + packaging cost → markup by channel |
| `is_per_kg` / per piece | Bulk goods (kg-based) vs. packaged goods (per piece, per jar, per pack) |
| Channels | Online (Shopee/Facebook), local sari-sari stores (reseller), personal delivery (offline) |
| Employees | Production helpers paid per production day |
| Active SRPs | Price list for different pack sizes — printable for market stalls |

### Key adaptations
- `product_id` becomes a product + pack-size combination (e.g., `ATCHARA_250G`, `ATCHARA_500G`).
- Operations log captures **yield per production run** — the quantity of finished goods output from a set of acquired ingredients becomes the basis for per-unit cost.
- Spoilage rate applies to shelf life — products approaching expiry are discounted.
- Pricing presets updated seasonally based on raw ingredient price movements.

### Specific value
Home producers usually guess pricing. This gives them a formal ingredient-cost-to-retail-price pipeline, a production log that doubles as a DOST/FDA batch record, and a multi-channel price list they can print or share as an image.

### Existing solutions
**Craftybase** (web-only, $39–$119/month) is the strongest globally — it does recipe costing, batch tracking, and COGS per unit. But it has no native Android app, no offline mode, no thermal receipt printing, and USD pricing makes it impractical for Philippine home producers. **Castiron** is a sales front-end only with no costing. No Filipino-context cottage food production app exists.

---

## 5. Barangay / Community Cooperative Store

**Business:** A community-run cooperative store or sari-sari store network with member credit (utang), bulk purchases from distributors, and profit sharing at year-end.

### Concept mapping

| Farm App concept | Cooperative equivalent |
|---|---|
| Acquisition | Bulk purchase from distributor or Cash & Carry; per-unit cost recorded |
| SRP per channel | Regular price (non-member), member price (discounted), reseller (other stores buying from coop) |
| Customers | Cooperative members with `customer_type = MEMBER` |
| Employee payments + cash advance | Member loans / credit advances against share capital; `cash_advance_amount` = credit released; `liquidated_amount` = credit repaid |
| Remittances | Payments to distributors or supplier representatives |
| Employees | Coop store staff with wages |
| Multi-device server sync | Multiple branch stores contributing to consolidated inventory and sales data |
| Export / reporting | Board of directors' quarterly report on sales, cost of goods, and member credit |

### Key adaptations
- `CustomerType` extended to include `MEMBER` with a `share_capital` attribute.
- Credit limit enforcement: if `outstanding_advance > credit_limit`, order blocked or flagged.
- `ADMIN` role maps to the cooperative Board Treasurer; `STORE_ASSISTANT` maps to store clerks.
- Product categories organized by distributor/supplier for re-order tracking.

### Specific value
Most small cooperatives run on manual ledger books or basic Excel. This gives them a real POS with member credit tracking, a server-consolidated view across branches, and board-ready reports — all on affordable Sunmi hardware already used by POS-forward coops.

### Existing solutions
**Peddlr** (free) has a basic credit ledger (utang tracking) and is offline-first with thermal printing — the best available generic option. **SmartSari** and **UTAK POS** cover inventory and sales but have no cooperative-specific features: no member share capital, no patronage refund calculation, no multi-owner reporting. No app exists that treats the customer as a *member* with both purchasing and ownership attributes.

---

## 6. Flower / Ornamental Plant Business

**Business:** A flower shop or ornamental plant retailer that buys from Dangwa or farm growers, arranges and packs for retail, and sells through walk-in, online orders, and bulk event buyers.

### Concept mapping

| Farm App concept | Flower shop equivalent |
|---|---|
| Acquisition | Flower / plant purchase from Dangwa market or farm — per stem, per bunch, per pot |
| Spoilage rate | Critically high and time-sensitive — roses last 3–5 days; spoilage rate varies by species |
| `is_per_kg` = false, `piece_count` | Stems per bunch; plants per tray |
| SRP channels | Walk-in retail (offline), online delivery (online), events/bulk (reseller) |
| Farm operations | Conditioning logs, water-changing, arrangement production, cold room checks |
| Employees | Florists, delivery riders — daily or per-event wages with cash advance |
| Active SRPs | Price board by flower type and arrangement size, printed per booking |

### Key adaptations
- Add **days-to-peak** and **days-to-expiry** derived from `date_acquired` displayed in the acquisitions list — triggers automatic spoilage-rate escalation for pricing.
- `product_id` represents species + grade (e.g., `ROSE_PREMIUM`, `ROSE_STANDARD`).
- Add **event booking** as an order type with a `scheduled_delivery_date` — analog of `is_delivered`.
- Arrangement production tracked as farm operations with ingredient deduction from acquired stems.

### Specific value
Flower pricing is intuition-driven because spoilage creates pressure to discount aging stock. The spoilage-adjusted SRP engine gives florists a data-driven markdown schedule, and the operations log creates a record of arrangement production that helps them cost custom orders accurately.

### Existing solutions
**Hana Florist POS** (web + mobile, freemium), **QuickFlora** ($49+/month + ~$2,000 setup), and **Floranext** (~$99/month) are the category leaders. All three are cloud-dependent, US/global market oriented with wire service (FTD/Teleflora) integration, priced in USD, and entirely absent of a per-stem spoilage-adjusted cost engine. For a small Philippine flower stall buying from Dangwa market, none of these are usable or affordable.

---

## 7. Water Refilling Station / Delivery Route Business

**Business:** A water refilling station operating multiple delivery routes, collecting empty containers, and running daily route-based cash collection.

### Concept mapping

| Farm App concept | Water delivery equivalent |
|---|---|
| Acquisition | Chemical supplies (salt, filters, resin), container purchases, cap/seal stock — per piece |
| `is_per_kg` = false, `piece_count` | Containers per delivery route (slim, round, dispenser sizes) |
| Orders | Delivery orders — customer, container type, quantity, route |
| Channels | Regular delivery (offline), standing subscription (reseller-equivalent), walk-in refill (online-equivalent) |
| Remittances | Daily driver cash remittance to store owner |
| Employees | Delivery drivers — daily wage + per-container incentive; cash advance common |
| Farm operations | Refilling production logs: gallons produced, TDS reading, machine maintenance |
| `FarmOperationType` | `REFILL_RUN`, `MAINTENANCE`, `TDS_READING`, `FILTER_CHANGE`, `CONTAINER_INVENTORY` |

### Key adaptations
- Add **route** as a field on orders — a named delivery area (Barangay X, Zone 3).
- Remittance workflow becomes the end-of-day driver settlement: expected collection vs. actual cash turned over.
- Container deposit tracking: customer holds N containers with a deposit; outstanding deposit tracked per customer.
- Spoilage rate repurposed as **production loss rate** (water wasted in filtering / rejected batches).

### Specific value
Water station owners struggle with driver accountability — cash collected does not always match deliveries made. The order-to-remittance pipeline creates a closed-loop: order recorded → delivery confirmed → cash remitted → discrepancy visible on the server dashboard.

### Existing solutions
**Smart Refill** (Philippines) and **Ka-tubig** are the most relevant but both are cloud-dependent platforms with no offline driver mode and no thermal receipt printing. **WaterDelivery.ph** is a consumer-facing order app only. Container deposit tracking (how many 5-gallon jugs are at each customer's home) and cost-per-gallon-produced computation exist in no current app.

---

## 8. Fuel Dealer / LPG Cylinder Distributor

**Business:** An authorized LPG dealer or fuel distributor managing cylinder inventory, refill orders, driver delivery routes, and cash collection from sari-sari stores and households.

### Concept mapping

| Farm App concept | LPG dealer equivalent |
|---|---|
| Acquisition | Cylinder stock purchase from Petron / Shell / Total depot — per cylinder (piece), purchase price per unit |
| `piece_count` | Kg per cylinder size (11 kg, 22 kg) — maps to per-piece with a known kg equivalent |
| SRP computation | Depot price per kg ÷ cylinder size + distribution cost → per-cylinder SRP |
| Channels | Household delivery (offline), sari-sari store reseller (reseller), walk-in exchange (online-equivalent) |
| Orders | Cylinder exchange orders — customer, cylinder size, quantity, delivery address |
| Remittances | Driver end-of-day cash collection settlement |
| Employees | Delivery drivers, warehouse staff |
| Farm operations | Cylinder leak testing, inventory count, depot trip logs |

### Key adaptations
- `product_id` is cylinder size (`LPG_11KG`, `LPG_22KG`, `LPG_50KG`).
- Add **cylinder deposit tracking** per customer (held cylinder count and deposit amount).
- `is_per_kg` = false with `piece_count` = kg per cylinder enables the pricing engine to compute cost per kg and then price per cylinder naturally.
- Pricing presets updated whenever the depot price changes — the admin activates a new preset and all devices immediately get updated SRPs.

### Specific value
LPG prices change frequently with every depot advisory. The preset activation → device sync pipeline means all drivers get updated selling prices instantly without phone calls, and management has a server record showing what price was active on each delivery date.

### Existing solutions
**LPG Ledger** (Android, freemium) tracks filled vs. empty cylinder stock and does basic POS billing — the only dedicated Android option. **GasFlow by Alizent** is enterprise-grade RFID cylinder tracking for large utilities, not micro-distributors. Neither has route-based delivery management, offline receipt printing, cylinder deposit tracking per customer, or a cost-from-depot-price-to-selling-price computation engine.

---

## 9. Ukay-Ukay / Secondhand Goods Retailer

**Business:** A secondhand clothing or general goods retailer that buys baled goods per kilo, sorts into categories, and prices by piece or by kilo for retail.

### Concept mapping

| Farm App concept | Ukay-ukay equivalent |
|---|---|
| Acquisition | Bale purchase — price per kg, total weight, supplier |
| Spoilage rate | Unsellable items (damaged, non-marketable) — estimated percentage of each bale |
| `is_per_kg` | Some goods sold per kg (linis pile); others sold per piece (select items) |
| SRP computation | (Bale cost ÷ sellable kg) + sorting labor → per-kg and per-piece SRP by category |
| Channels | Walk-in (offline), online (Facebook Live auction / Shopee), bulk buyer (reseller) |
| Farm operations | Bale opening logs, sorting sessions, category count, display replenishment |
| Employees | Sorters, cashiers — daily wage with cash advance |
| Active SRPs | Per-kg price board by category (Tops, Bottoms, Dress, etc.) — thermal printable |

### Key adaptations
- `product_id` represents clothing category + grade (e.g., `TOPS_SELECT`, `TOPS_LINIS`, `SHOES_MIXED`).
- Spoilage = rejection rate when sorting — rate varies widely by bale origin.
- `piece_count` repurposed: estimated sellable pieces per kg for select items.
- Operations log used for bale opening events: bale ID, source, gross weight, sorted output per category.

### Specific value
Ukay-ukay pricing is almost entirely informal. This gives operators a cost-per-bale → cost-per-kg-sellable → SRP computation that accounts for sorting losses, a multi-channel price structure (different prices for online auctions vs. walk-in), and a server-consolidated view if running multiple branches.

### Existing solutions
**No dedicated ukay-ukay POS app exists** on the Play Store or App Store. Sellers use Shopee/Lazada seller apps (online only, no physical POS), Facebook Marketplace (entirely manual), or general-purpose Peddlr for basic cash transactions. None handle bundle acquisition costing, sorting-loss spoilage, per-bale unit-cost derivation, or fast bulk-transaction POS (e.g., "5 pieces for PHP 150"). This is the single most underserved category in this list.

---

## 10. School Canteen / University Food Concession

**Business:** A school canteen concessionaire or university food kiosk operating multiple serving counters, buying ingredients daily, and selling prepared food to students and staff.

### Concept mapping

| Farm App concept | Canteen equivalent |
|---|---|
| Acquisition | Daily market run — vegetables, meat, condiments, packaging materials — per kg and per piece |
| Spoilage rate | Leftover / unsold portions at end of service — daily waste rate per dish |
| SRP computation | Ingredient cost per serving ÷ portion yield + labor overhead → menu price per channel |
| Channels | Student price (offline), faculty/staff price (reseller-equivalent discount), takeout/delivery (online) |
| Orders | Tray orders (walk-up counter) or advance orders (pre-ordered packed lunches) |
| Farm operations | Cooking logs: dishes prepared, portions yielded, leftover quantity |
| Employees | Kitchen staff and cashiers — with daily wages and cash advance |
| Remittances | Daily cash remittance from each counter to the concessionaire manager |
| Multi-device | One Sunmi device per counter; server consolidates all counter sales |

### Key adaptations
- `product_id` is a menu item (`ADOBO_RICE_MEAL`, `PANCIT_SOLO`, `SOFTDRINK_SMALL`).
- `is_per_kg` = false for all prepared food — everything sold per piece/serving.
- Acquisition is ingredient-level; SRP is menu-item-level — a **recipe mapping** layer links acquired ingredients to prepared dishes (analog of the pricing preset's `channels_json` carrying the ingredient-to-dish ratio).
- Operations log records each cooking batch: dish name, ingredient batch used, portions produced — the yield figure feeds back into SRP validation.
- Spoilage rate captures daily leftover percentage — a canteen that wastes 20% of adobo per day should price differently than one that sells out.

### Specific value
School canteen concessionaires often price by gut feel and lose money without realizing it. The ingredient-cost-to-menu-price pipeline with portion yield and spoilage makes food costing rigorous. The multi-counter server consolidation gives the concessionaire a daily P&L view across all serving stations — which no basic POS provides.

### Existing solutions
**CanteenPOS** (iOS-only) targets cashless card/app payments — not suitable for Philippine cash-based canteens. **PaySchools** and **Vanco** are US public school lunch systems entirely. General tablet POS apps (UTAK, StoreHub) are used ad-hoc but have no ingredient costing, no per-serving yield tracking, and no multi-stall consolidation at an affordable price point. BIR OR printing is unaddressed by all options.

---

## 11. Carinderia / Small Filipino Eatery

**Business:** A neighborhood carinderia, turo-turo, or small eatery that buys raw ingredients daily from the wet market, cooks multiple viands, and sells by the serving (tapak) or by the plate.

### Concept mapping

| Farm App concept | Carinderia equivalent |
|---|---|
| Acquisition | Daily market run — pork, chicken, vegetables, cooking oil, rice — per kg and per piece |
| Spoilage rate | Unsold viands at the end of service — leftover rate varies per dish |
| SRP computation | Ingredient cost per serving ÷ portion yield + gas/cooking labor overhead → menu price |
| `is_per_kg` = false | All food sold per serving (piece), not by weight |
| Channels | Dine-in (offline), pre-order / packed lunch (online), catering (reseller-equivalent) |
| Farm operations | Cooking batch logs: dish name, portions made, leftover count at closing time |
| Employees | Cook, cashier — daily wages, common cash advance |
| Remittances | End-of-day cash remittance from cashier to owner |
| Multi-device | Main counter device + kitchen display or owner's monitoring device |

### Key adaptations
- **Recipe-to-acquisition link:** A `recipe_json` field on each product (menu item) maps ingredient IDs and quantities per serving — the pricing engine uses this to compute ingredient cost per serving from the day's acquisition prices.
- Pricing preset activated each morning after the market run, not quarterly.
- `FarmOperationType` extended to include `COOKING_BATCH`, `LEFTOVER_RECORDED`, `CLOSING_COUNT`.
- Add a **daily P&L summary** operation: total acquisition cost vs. total sales, implied margin per viand.

### Specific value
A carinderia owner who spends PHP 3,500 on ingredients each day and makes PHP 4,200 in sales does not know which viand is profitable and which is a loss. The ingredient-cost-to-selling-price engine makes this visible. The leftover/spoilage tracking shows which dishes consistently result in waste — a direct input for menu planning.

### Existing solutions
**CukCuk POS** (~PHP 1,200–2,500/month) and **EngagePOS** (~PHP 2,250/month) are Philippines-built restaurant POS apps with offline capability and BIR compliance. **Peddlr** is used by simpler carinderias for free. Critical gap in all three: **no ingredient costing or menu pricing engine**. None take today's pork price from the market, apply a recipe portion yield, and compute the minimum selling price per viand. Owners set prices based on what neighbors charge, not what their cost structure requires.

---

## 12. Talipapa / Ambulant Market Vendor

**Business:** A mobile or ambulant vendor who buys goods in the early morning from a wholesale source and sells throughout the day at a fixed spot in a talipapa, barangay road, or market perimeter — often with a changing location or a route.

### Concept mapping

| Farm App concept | Ambulant vendor equivalent |
|---|---|
| Acquisition | Early-morning purchase from the wholesale market — per kg, per bundle, per sack |
| Spoilage rate | Unsold goods at day's end — for perishables (fish, vegetables), rate is high and fast |
| SRP per channel | Passersby (offline), suki regulars at a slight discount (reseller), online pre-order for pickup |
| Active SRPs screen | Verbal price reference or chalked price board — now printable or screen-displayable |
| Farm operations | End-of-day closing: unsold quantity counted, spoilage recorded, proceeds tallied |
| Employees | Typically solo or one helper — daily wage and cash advance |
| Remittances | End-of-day remittance to a business partner or cooperative fund |

### Key adaptations
- **Daily repricing workflow** is the core differentiator: a single screen where the vendor enters today's acquisition cost per item and the app recomputes all SRPs — replacing what is currently done on a scrap of paper or not done at all.
- Pricing preset activated fresh each morning based on the day's purchase price — no preset lasts more than one day.
- Offline-first is non-negotiable: talipapa vendors often have poor or no connectivity mid-day.
- Add a **"quick sell"** mode in the order screen: no customer selection, fast item + quantity + total, receipt printed to Bluetooth thermal. Transaction speed is the primary UX constraint.
- `AcquisitionLocation` = `WHOLESALE_MARKET` | `FARM_GATE` | `SUBASTA`.

### Specific value
Ambulant vendors operate on thin margins with perishable goods and no pricing discipline. Most price by instinct or by copying neighbors. This app gives any vendor a 30-second morning workflow: enter today's price → get today's SRP → sell at the right price → know at the end of the day whether they made money.

### Existing solutions
**Peddlr** (free, offline, thermal printing) is the closest available tool. It handles basic cash POS transactions well. The gap: Peddlr has no acquisition cost entry, no daily repricing workflow, and no spoilage-adjusted SRP engine. A vendor using Peddlr still sets prices manually with no connection to what they paid that morning. **HitPay Mobile POS** requires internet for payments. No app exists that solves the ambulant vendor's specific problem: acquisition cost → SRP computation → fast cash POS → end-of-day reconciliation, all offline on a phone.

---

## 13. Hardware / Construction Supplies Micro-Retailer

**Business:** A small hardware store or construction supply retailer that buys cement, steel bars, hollow blocks, paint, nails, and fittings in bulk (by sack, by ton, by box) and sells by the piece, by the length, or by the kilo.

### Concept mapping

| Farm App concept | Hardware store equivalent |
|---|---|
| Acquisition | Supplier purchase — cement per sack, deformed bars per bundle (6m length), paint per can |
| `is_per_kg` / `piece_count` | Per-sack goods with a per-kg equivalent (cement 40kg/sack); per-piece goods (rebars, pipes) |
| SRP computation | Cost per sack ÷ yield units (e.g., per bag, per meter of pipe) + delivery cost → markup by channel |
| Channels | Walk-in retail (offline), contractor discount (reseller), project quotation (online-equivalent) |
| Orders | Quotation-to-delivery orders for construction projects — large quantities, credit terms |
| Customers | Contractors as reseller customers with credit accounts; homeowners as retail |
| Employees | Counter staff, delivery crew — wages with cash advance |
| Remittances | Credit payment collection from contractor accounts |

### Key adaptations
- **Unit conversion** is the key new concept: one acquisition entry (e.g., 1 bundle of 10 deformed bars at PHP 4,800) must auto-generate per-piece and per-kilo SRP alternatives. A `unit_conversion_factor` field on products handles this.
- `product_id` includes size/grade: `REBAR_10MM_6M`, `HOLLOW_BLOCK_4IN`, `CEMENT_TYPE1_40KG`.
- Credit account management for contractors: `customer_credit_limit`, outstanding balance tracked via orders and remittances.
- `AcquisitionLocation` = `DIRECT_SUPPLIER` | `HARDWARE_WHOLESALER` | `DEPOT`.

### Specific value
Small hardware store owners lose money on contractor accounts because they extend credit without tracking it and underprice items because they never formally compute cost-per-unit from their bulk purchase price. The pricing engine's bulk-cost → per-unit SRP computation with delivery cost overlay solves the pricing problem; the customer + remittance workflow solves the credit tracking problem.

### Existing solutions
**GoFrugal RetailEasy** (subscription, not publicly priced for Philippines) is the most capable globally — offline sync, hardware-shop-specific workflows, multi-branch. But it requires a PC or dedicated tablet terminal, does not natively support Bluetooth thermal printing on Android, and is subscription-priced for organized retailers — not the PHP 50,000-capitalized micro-hardware store. **Peddlr** and **Imonggo POS** (free–PHP 1,662/month) are used by small stores but have no unit conversion, no cost-to-SRP, and no contractor credit ledger. No Philippines-built hardware-specific app exists.

---

## 14. Laundry / Wash-Dry-Fold Service

**Business:** A neighborhood laundry shop or wash-dry-fold service that accepts laundry by the kilo, charges per kg, may offer pickup and delivery, and manages daily operating costs (water, electricity, detergent, fabric conditioner).

### Concept mapping

| Farm App concept | Laundry shop equivalent |
|---|---|
| Acquisition | Consumables purchase: detergent powder, fabric conditioner, plastic bags — per kg or per piece |
| SRP computation | (Consumable cost per kg of laundry + water/electricity overhead) ÷ machine load yield → price per kg |
| `is_per_kg` = true | Laundry accepted and priced by weight |
| Orders | Laundry order: customer, weight (kg), service type (wash-dry-fold, wash-dry only), delivery flag |
| Channels | Walk-in (offline), home pickup/delivery (online), hotel/dorm bulk (reseller) |
| Farm operations | Machine cycle logs: load count, detergent used, power consumption notes, machine maintenance |
| Employees | Washers, folders, delivery riders — daily wages with cash advance |
| Remittances | Rider end-of-day cash remittance for delivery collections |
| Customers | Regulars (residents), bulk accounts (hotels, dormitories, restaurants) |

### Key adaptations
- **Weight-at-intake** is the order anchor — the `quantity` field on an order is the actual weighed kg of the customer's laundry. The price is computed as `quantity × srp_per_kg`.
- Operations log repurposed for machine maintenance: `MACHINE_MAINTENANCE`, `POWER_OUTAGE_LOG`, `DETERGENT_STOCK_COUNT`.
- Add **order status stages** beyond paid/delivered: `RECEIVED → WASHING → DRYING → READY → DELIVERED`. The current `is_paid` / `is_delivered` boolean pair can be extended.
- Pricing preset updated when LBC electricity rates or detergent prices change — captures the cost-input-to-price-output link that owners currently ignore.

### Specific value
Laundry shop owners in the Philippines typically set a flat PHP 55–75/kg rate with no calculation behind it. This app gives them a cost-to-SRP engine for their specific consumable and utility costs, a customer order tracker with thermal receipt printing at intake, and a delivery rider remittance loop. The server-side view consolidates multiple branches or partner laundry drops into one dashboard.

### Existing solutions
**CleanCloud** (subscription, starts ~$35–$75/month in USD) is the global category leader — order management, barcode garment tracking, delivery routing, performance reports. It is cloud-dependent with no offline mode (critical during power outages common in Philippine provinces), priced in USD (unaffordable for most local shops), and has no thermal Bluetooth receipt printing. **Geelus** and **TURNS** have the same cloud-dependency problem. **No Philippines-local, offline-first, peso-denominated laundry shop app exists** for the PHP 55/kg neighborhood laundry that represents the vast majority of the market.

---

## 15. Rice and Grain Dealer / NFA Accredited Retailer

**Business:** A rice retailer or grain dealer (palay, corn, mongo, munggo, dried beans) that buys in bulk by the cavan or sack from traders or NFA, and retails by the kilo at varying prices per variety and grade.

### Concept mapping

| Farm App concept | Rice dealer equivalent |
|---|---|
| Acquisition | Cavan purchase from NFA or trader — price per cavan varies by variety (Sinandomeng, Dinorado, RC) and grade (special, well-milled, regular) |
| `is_per_kg` = true, `piece_count` | Price per cavan ÷ kilo conversion (1 cavan = 50 kg) → cost per kg |
| Spoilage rate | Moisture loss, insect damage, broken grain — typically 3–8% per sack |
| SRP computation | Cost per kg (cavan price ÷ 50 kg) + spoilage adjustment + transport → SRP per kg |
| Channels | Retail (walk-in, per kg), sari-sari store reseller (per sack discount), online pre-order (bags pre-packed) |
| Active SRPs screen | Price board by rice variety — printable for counter display |
| Employees | Counter staff, delivery assistants — daily wage and cash advance |
| Remittances | Payments to cavan suppliers or NFA agents |
| Farm operations | Stock count logs, moisture meter readings, pest inspection entries |

### Key adaptations
- `product_id` encodes variety + grade: `RICE_SINANDOMENG_SPECIAL`, `RICE_DINORADO_WELLMILLED`, `CORN_GRITS_FINE`.
- **Cavan-to-kilo conversion** is built into `piece_count`: for rice, `piece_count = 50` (kg per cavan) makes the pricing engine compute cost-per-kg from cost-per-cavan automatically — the same per-piece math already in the system.
- Pricing preset activated whenever a new cavan purchase arrives at a different price — potentially multiple times per week during harvest and lean season price swings.
- Add a **stock balance** view: total cavans acquired − total kg sold = estimated remaining stock per variety.
- `AcquisitionLocation` = `NFA_WAREHOUSE` | `TRADER` | `FARM_GATE` | `PALAY_BUYER`.

### Specific value
Rice dealers operate on extremely thin margins (PHP 1–5/kg) and price swings from the NFA or trader level directly compress profitability if the retail price is not updated quickly. The preset activation → instant SRP broadcast to all counter devices means a price change flows to all staff immediately. The spoilage-adjusted cost computation makes explicit the grain loss that dealers currently absorb invisibly into their margins.

### Existing solutions
No dedicated rice dealer management app exists on the Play Store. Rice dealers in the Philippines use physical notebooks, calculator apps, or basic Excel. Some use **Peddlr** for cash POS transactions but without any cavan-to-kilo conversion, acquisition cost tracking, or SRP computation. The NFA has no retailer-facing digital tool for accredited dealers. This is a completely unserved segment with an extraordinarily large addressable market — rice retailing is among the most common micro-businesses in every Philippine municipality.

---

## Summary

| # | Business | Core reuse | Key adaptation | Best existing app | Critical gap |
|---|---|---|---|---|---|
| 1 | Wet Market Stall | Full — direct analog | Daily preset; daily closing op | Peddlr (free) | No cost-to-SRP engine |
| 2 | Fish Port / Seafood | Pricing engine, spoilage, channels | Time-to-expiry, species/grade | Markt POS (US, cloud) | No PH offline mobile solution |
| 3 | Poultry / Livestock | Acquisition, ops log, pay | Batch lifecycle, mortality rate | My Poultry Manager | No pricing recommendation from costs |
| 4 | Cottage Food | Pricing engine, ops, channels | Yield-from-production costing | Craftybase (web, USD) | No native Android, no thermal print |
| 5 | Community Cooperative | Orders, customers, sync | Member credit, share capital | Peddlr (free) | No cooperative-specific features |
| 6 | Flower Shop | Spoilage-adjusted SRP, ops | Days-to-expiry escalation | Hana / Floranext (cloud, USD) | No PH affordable offline solution |
| 7 | Water Delivery | Orders, remittances, pay | Route, container deposit | Smart Refill (PH, cloud) | No offline driver + thermal receipt |
| 8 | LPG Distributor | Pricing engine, presets, remit | Cylinder deposit, per-unit SRP | LPG Ledger (basic) | No route + deposit + cost engine |
| 9 | Ukay-Ukay | Acquisition, spoilage, channels | Bale-opening ops, category SRP | **None** | No dedicated app exists at all |
| 10 | School Canteen | Full — multi-device, pricing | Recipe mapping, per-counter remit | CanteenPOS (iOS only) | No cash-first offline PH solution |
| 11 | Carinderia / Eatery | Acquisition, ops, remittances | Recipe-to-ingredient link | CukCuk / EngagePOS (PH) | No ingredient costing engine |
| 12 | Ambulant / Talipapa | Full — fast daily cycle | Quick-sell mode, daily repricing | Peddlr (free) | No acquisition-cost-to-SRP workflow |
| 13 | Hardware Store | Orders, customers, pay | Unit conversion, contractor credit | GoFrugal (PC-based) | No mobile thermal + credit ledger |
| 14 | Laundry Shop | Orders, remittances, ops | Weight-at-intake, machine log | CleanCloud (cloud, USD) | No offline PH peso solution |
| 15 | Rice / Grain Dealer | Pricing engine, presets, channels | Cavan-to-kilo conversion, stock log | **None** | No dedicated app exists at all |

---

## Market Gap Analysis

Research across all 15 categories reveals one universal finding: **no existing app — on the Play Store, App Store, or as a web application — provides a cost-to-SRP pricing engine for commodity-based micro-businesses**.

Every app in the market starts at the selling price and works forward (POS, inventory deduction, sales reporting). None start at the purchase price and work backward through spoilage, overhead, and margin to arrive at a defensible selling price. This is the architectural differentiator.

**Secondary gaps consistent across categories:**

| Gap | How common | Apps that address it |
|---|---|---|
| Offline-first Android | 12 of 15 categories underserved | Peddlr, some poultry apps |
| Bluetooth thermal printing | 13 of 15 categories underserved | Peddlr only |
| Cost-to-SRP computation | **15 of 15 categories — universal** | **None** |
| Multi-device sync at micro-business scale | 14 of 15 categories | Cloud-based only (connectivity required) |
| Spoilage / loss rate in pricing | 15 of 15 categories | **None** |
| Philippines peso + local context | 10 of 15 categories | Peddlr, Parmazip, CukCuk, BitPOS |
| Credit ledger (utang / account customers) | 10 of 15 categories | Peddlr (basic) |

**The two completely unserved categories** (no dedicated app exists): Ukay-Ukay (#9) and Rice/Grain Dealer (#15). Both are among the most common micro-businesses in the Philippines.

**Peddlr** is the only app that partially addresses the horizontal need (offline, thermal, PHP, credit) but is a generic platform with no vertical depth — it cannot compute what price a fish vendor should charge today based on what they paid at 4am and what their expected spoilage rate is.

The through-line across all 15: a small operation buying goods at variable bulk cost, selling through multiple channels with loss and spoilage, managing staff, and having no structured tool to connect what they paid to what they should charge — and no consolidated view of whether the business made money today.
