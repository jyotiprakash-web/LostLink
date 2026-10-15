import matplotlib.pyplot as plt
import matplotlib.patches as patches

def create_architecture_diagram():
    fig, ax = plt.subplots(figsize=(12, 7.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    # Background canvas
    fig.patch.set_facecolor('#F8FAFC')
    ax.set_facecolor('#F8FAFC')

    # Title
    ax.text(50, 96, "LostLink System Architecture & Multi-Layer Design", 
            ha='center', va='center', fontsize=16, fontweight='bold', color='#0F172A')

    # Layer 1: Presentation Layer
    rect_pres = patches.FancyBboxPatch((10, 80), 80, 11, boxstyle="round,pad=1.2", 
                                       facecolor='#DBEAFE', edgecolor='#2563EB', linewidth=2)
    ax.add_patch(rect_pres)
    ax.text(50, 87, "PRESENTATION LAYER (Terminal & Console UI)", 
            ha='center', va='center', fontsize=12, fontweight='bold', color='#1E40AF')
    ax.text(50, 83, "Main.java (Menu Shell, Routing, 9 Submenus)   |   InputHelper.java (Stream-Injected Scanner & Validation)", 
            ha='center', va='center', fontsize=10, color='#1E3A8A')

    # Arrow from Layer 1 to Layer 2
    ax.annotate('', xy=(50, 71.5), xytext=(50, 79.5),
                arrowprops=dict(facecolor='#2563EB', edgecolor='#2563EB', width=2, headwidth=8))

    # Layer 2: Service / Business Logic Layer
    rect_svc = patches.FancyBboxPatch((5, 47), 90, 24, boxstyle="round,pad=1.2", 
                                      facecolor='#FEF3C7', edgecolor='#D97706', linewidth=2)
    ax.add_patch(rect_svc)
    ax.text(50, 68, "BUSINESS LOGIC & SERVICE LAYER", 
            ha='center', va='center', fontsize=12, fontweight='bold', color='#92400E')

    # Service Sub-boxes
    # ItemService
    b_item = patches.FancyBboxPatch((8, 50), 25, 15, boxstyle="round,pad=0.8", 
                                    facecolor='#FFFBEB', edgecolor='#F59E0B', linewidth=1.5)
    ax.add_patch(b_item)
    ax.text(20.5, 62, "ItemService.java", ha='center', va='center', fontsize=11, fontweight='bold', color='#78350F')
    ax.text(20.5, 57, "- In-memory itemList (List)\n- Fast itemMap (Map)\n- ID Generator (ITM-xxxx)\n- Synchronized mutations", 
            ha='center', va='center', fontsize=8.5, color='#451A03')

    # MatchingService
    b_match = patches.FancyBboxPatch((37.5, 50), 25, 15, boxstyle="round,pad=0.8", 
                                     facecolor='#FFFBEB', edgecolor='#F59E0B', linewidth=1.5)
    ax.add_patch(b_match)
    ax.text(50, 62, "MatchingService.java", ha='center', va='center', fontsize=11, fontweight='bold', color='#78350F')
    ax.text(50, 57, "- 100-pt Heuristic formula\n- Token & keyword overlap\n- Location & date proximity\n- Top-3 Bounded Array", 
            ha='center', va='center', fontsize=8.5, color='#451A03')

    # ClaimService
    b_claim = patches.FancyBboxPatch((67, 50), 25, 15, boxstyle="round,pad=0.8", 
                                     facecolor='#FFFBEB', edgecolor='#F59E0B', linewidth=1.5)
    ax.add_patch(b_claim)
    ax.text(79.5, 62, "ClaimService.java", ha='center', va='center', fontsize=11, fontweight='bold', color='#78350F')
    ax.text(79.5, 57, "- Claim registration (CLM-xxxx)\n- Review workflow (Approve/Reject)\n- Handover completion\n- Cascade auto-rejection", 
            ha='center', va='center', fontsize=8.5, color='#451A03')

    # Cross-layer Domain Model side banner
    rect_model = patches.FancyBboxPatch((5, 33), 90, 10, boxstyle="round,pad=1.2", 
                                        facecolor='#DCFCE7', edgecolor='#16A34A', linewidth=2)
    ax.add_patch(rect_model)
    ax.text(50, 40, "DOMAIN ENTITIES & DATA TRANSFER OBJECTS (src/model)", 
            ha='center', va='center', fontsize=11, fontweight='bold', color='#14532D')
    ax.text(50, 35.5, "Item (abstract) <- LostItem, FoundItem (Confidential Details Isolated) | User <- Student, Staff\nClaim, MatchResult, SystemStatistics, ItemCategory, ItemStatus, ClaimStatus", 
            ha='center', va='center', fontsize=9, color='#166534')

    # Arrow from Layer 2 to Persistence
    ax.annotate('', xy=(50, 22.5), xytext=(50, 31.5),
                arrowprops=dict(facecolor='#059669', edgecolor='#059669', width=2, headwidth=8))

    # Layer 3: Persistence & Utilities Layer
    rect_store = patches.FancyBboxPatch((5, 3), 90, 19, boxstyle="round,pad=1.2", 
                                        facecolor='#F3E8FF', edgecolor='#9333EA', linewidth=2)
    ax.add_patch(rect_store)
    ax.text(50, 19, "PERSISTENCE & STORAGE UTILITIES (src/service/FileStorage.java & src/util/CsvCodec.java)", 
            ha='center', va='center', fontsize=11, fontweight='bold', color='#581C87')

    # Storage detail boxes
    b_s1 = patches.FancyBboxPatch((8, 5.5), 25, 10.5, boxstyle="round,pad=0.6", 
                                  facecolor='#FAF5FF', edgecolor='#A855F7', linewidth=1)
    ax.add_patch(b_s1)
    ax.text(20.5, 13, "data/items.csv", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#4C1D95')
    ax.text(20.5, 8.5, "12 RFC-4180 columns\nAtomic rewrite / temp swap", ha='center', va='center', fontsize=8, color='#6B21A8')

    b_s2 = patches.FancyBboxPatch((37.5, 5.5), 25, 10.5, boxstyle="round,pad=0.6", 
                                  facecolor='#FAF5FF', edgecolor='#A855F7', linewidth=1)
    ax.add_patch(b_s2)
    ax.text(50, 13, "data/claims.csv", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#4C1D95')
    ax.text(50, 8.5, "10 RFC-4180 columns\nReferential integrity checked", ha='center', va='center', fontsize=8, color='#6B21A8')

    b_s3 = patches.FancyBboxPatch((67, 5.5), 25, 10.5, boxstyle="round,pad=0.6", 
                                  facecolor='#FAF5FF', edgecolor='#A855F7', linewidth=1)
    ax.add_patch(b_s3)
    ax.text(79.5, 13, "Audit & Quarantine Logs", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#4C1D95')
    ax.text(79.5, 8.5, "activity.log (append-only)\n*.rejected (error quarantine)", ha='center', va='center', fontsize=8, color='#6B21A8')

    plt.tight_layout()
    plt.savefig('doc_assets/architecture_diagram.png', dpi=300, facecolor='#F8FAFC')
    plt.close()
    print("Saved architecture_diagram.png")

def create_workflow_diagram():
    fig, ax = plt.subplots(figsize=(12, 8.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.set_facecolor('#F8FAFC')

    ax.text(50, 97, "LostLink End-to-End Application Workflow", 
            ha='center', va='center', fontsize=16, fontweight='bold', color='#0F172A')

    # Start
    box_start = patches.FancyBboxPatch((40, 89), 20, 5, boxstyle="round,pad=0.5", 
                                       facecolor='#1E293B', edgecolor='#0F172A', linewidth=1.5)
    ax.add_patch(box_start)
    ax.text(50, 91.5, "Application Launch (Main)", ha='center', va='center', fontsize=10, fontweight='bold', color='#FFFFFF')

    # Startup Load
    box_load = patches.FancyBboxPatch((28, 80), 44, 5.5, boxstyle="round,pad=0.5", 
                                      facecolor='#E2E8F0', edgecolor='#64748B', linewidth=1.5)
    ax.add_patch(box_load)
    ax.text(50, 82.7, "Load items.csv & claims.csv (FileStorage) | Optional: --demo data", 
            ha='center', va='center', fontsize=9, fontweight='bold', color='#1E293B')

    ax.annotate('', xy=(50, 86), xytext=(50, 89),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', width=1.5, headwidth=6))

    # Menu Box
    box_menu = patches.FancyBboxPatch((25, 68), 50, 7.5, boxstyle="round,pad=0.6", 
                                      facecolor='#DBEAFE', edgecolor='#2563EB', linewidth=2)
    ax.add_patch(box_menu)
    ax.text(50, 72.5, "MAIN CONSOLE MENU (Options 1 - 9)", 
            ha='center', va='center', fontsize=11, fontweight='bold', color='#1E40AF')
    ax.text(50, 69.5, "Interactive Input Scanning & Strict Choice Validation via InputHelper", 
            ha='center', va='center', fontsize=8.5, color='#1E3A8A')

    ax.annotate('', xy=(50, 76), xytext=(50, 80),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', width=1.5, headwidth=6))

    # 4 Major Action Branches
    # Branch 1: Reporting (Opt 1 & 2)
    b1 = patches.FancyBboxPatch((3, 44), 20, 17, boxstyle="round,pad=0.6", 
                                facecolor='#ECFDF5', edgecolor='#10B981', linewidth=1.5)
    ax.add_patch(b1)
    ax.text(13, 58, "1 & 2: REPORT ITEM", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#065F46')
    ax.text(13, 50.5, "- Collect attributes\n- Validate fields & dates\n- Lost: Last seen details\n- Found: Private details\n- Assign ID ITM-xxxx\n- Status: OPEN", 
            ha='center', va='center', fontsize=8, color='#047857')

    # Branch 2: Search & Browse (Opt 4 & 5)
    b2 = patches.FancyBboxPatch((26, 44), 21, 17, boxstyle="round,pad=0.6", 
                                facecolor='#F0F9FF', edgecolor='#0EA5E9', linewidth=1.5)
    ax.add_patch(b2)
    ax.text(36.5, 58, "4 & 5: SEARCH & BROWSE", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#0369A1')
    ax.text(36.5, 50.5, "- Multi-token search\n- Category, color filter\n- Status filter\n- Comparator sorting\n  (date desc/asc, A-Z)\n- Private details hidden", 
            ha='center', va='center', fontsize=8, color='#0284C7')

    # Branch 3: Heuristic Match (Opt 3)
    b3 = patches.FancyBboxPatch((50, 44), 23, 17, boxstyle="round,pad=0.6", 
                                facecolor='#FEF3C7', edgecolor='#F59E0B', linewidth=1.5)
    ax.add_patch(b3)
    ax.text(61.5, 58, "3: HEURISTIC MATCHING", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#92400E')
    ax.text(61.5, 50.5, "- Select lost item\n- Calculate 100-pt score\n- Filter score >= 30\n- Deterministic tie-break\n- Display Top 3 array\n- One-click Claim Handoff", 
            ha='center', va='center', fontsize=8, color='#B45309')

    # Branch 4: Claims & Returns (Opt 6)
    b4 = patches.FancyBboxPatch((76, 44), 21, 17, boxstyle="round,pad=0.6", 
                                facecolor='#FEE2E2', edgecolor='#EF4444', linewidth=1.5)
    ax.add_patch(b4)
    ax.text(86.5, 58, "6: CLAIM LIFECYCLE", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#991B1B')
    ax.text(86.5, 50.5, "- Submit Claim (CLM-xxxx)\n- Staff review details\n- Compare private info\n- Approve / Reject / Info\n- Complete handover\n- Competing claims auto-x", 
            ha='center', va='center', fontsize=8, color='#B91C1C')

    # Connecting arrows from Menu to Branches
    ax.annotate('', xy=(13, 62), xytext=(35, 68),
                arrowprops=dict(facecolor='#2563EB', edgecolor='#2563EB', width=1.2, headwidth=5))
    ax.annotate('', xy=(36.5, 62), xytext=(45, 68),
                arrowprops=dict(facecolor='#2563EB', edgecolor='#2563EB', width=1.2, headwidth=5))
    ax.annotate('', xy=(61.5, 62), xytext=(55, 68),
                arrowprops=dict(facecolor='#2563EB', edgecolor='#2563EB', width=1.2, headwidth=5))
    ax.annotate('', xy=(86.5, 62), xytext=(65, 68),
                arrowprops=dict(facecolor='#2563EB', edgecolor='#2563EB', width=1.2, headwidth=5))

    # Bottom Operations: My Reports (7), Statistics (8), Save & Exit (9)
    b_dash = patches.FancyBboxPatch((15, 23), 32, 14, boxstyle="round,pad=0.6", 
                                    facecolor='#EDE9FE', edgecolor='#8B5CF6', linewidth=1.5)
    ax.add_patch(b_dash)
    ax.text(31, 33.5, "7 & 8: REPORTS & DASHBOARD", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#5B21B6')
    ax.text(31, 28, "- Filter reports by reporter name\n- Edit details on OPEN items\n- Total lost, found, returned counts\n- Safe recovery rate calculation", 
            ha='center', va='center', fontsize=8, color='#6D28D9')

    b_exit = patches.FancyBboxPatch((53, 23), 32, 14, boxstyle="round,pad=0.6", 
                                    facecolor='#F1F5F9', edgecolor='#475569', linewidth=1.5)
    ax.add_patch(b_exit)
    ax.text(69, 33.5, "9: SAVE DATA & EXIT", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#1E293B')
    ax.text(69, 28, "- Atomic file swap (items.csv)\n- Atomic file swap (claims.csv)\n- Flush append-only activity.log\n- Clean shutdown with zero data loss", 
            ha='center', va='center', fontsize=8, color='#334155')

    # Return loop arrow
    ax.annotate('Loop back to Menu', xy=(25, 72), xytext=(6, 28),
                arrowprops=dict(facecolor='#64748B', edgecolor='#64748B', width=1.2, headwidth=5, connectionstyle="arc3,rad=-0.4"),
                fontsize=8, color='#475569', fontweight='bold')

    # Final Exit node
    box_end = patches.FancyBboxPatch((40, 5), 20, 6, boxstyle="round,pad=0.5", 
                                     facecolor='#0F172A', edgecolor='#020617', linewidth=1.5)
    ax.add_patch(box_end)
    ax.text(50, 8, "Program Terminated (Exit 0)", ha='center', va='center', fontsize=9.5, fontweight='bold', color='#FFFFFF')

    ax.annotate('', xy=(50, 11.5), xytext=(69, 22.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', width=1.2, headwidth=5))

    plt.tight_layout()
    plt.savefig('doc_assets/workflow_flowchart.png', dpi=300, facecolor='#F8FAFC')
    plt.close()
    print("Saved workflow_flowchart.png")

def create_claim_lifecycle_diagram():
    fig, ax = plt.subplots(figsize=(11, 7.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.set_facecolor('#F8FAFC')

    ax.text(50, 96, "Claim Lifecycle State Machine & Item Synchronization", 
            ha='center', va='center', fontsize=15, fontweight='bold', color='#0F172A')

    # State: PENDING
    b_pending = patches.FancyBboxPatch((35, 76), 30, 12, boxstyle="round,pad=0.8", 
                                       facecolor='#FEF3C7', edgecolor='#D97706', linewidth=2)
    ax.add_patch(b_pending)
    ax.text(50, 83.5, "CLAIM: PENDING", ha='center', va='center', fontsize=11, fontweight='bold', color='#92400E')
    ax.text(50, 79, "Trigger: submitClaim(foundId, claimant, info)\nFoundItem -> CLAIM_PENDING", 
            ha='center', va='center', fontsize=8.5, color='#78350F')

    # State: INFO_REQUESTED (self-loop / notes)
    b_info = patches.FancyBboxPatch((5, 48), 26, 12, boxstyle="round,pad=0.8", 
                                    facecolor='#E0F2FE', edgecolor='#0284C7', linewidth=2)
    ax.add_patch(b_info)
    ax.text(18, 55.5, "INFO REQUESTED", ha='center', va='center', fontsize=10.5, fontweight='bold', color='#0369A1')
    ax.text(18, 51, "Status: Still PENDING\nReview notes recorded\nItem stays CLAIM_PENDING", 
            ha='center', va='center', fontsize=8, color='#075985')

    # State: REJECTED
    b_rej = patches.FancyBboxPatch((69, 48), 26, 12, boxstyle="round,pad=0.8", 
                                   facecolor='#FEE2E2', edgecolor='#DC2626', linewidth=2)
    ax.add_patch(b_rej)
    ax.text(82, 55.5, "CLAIM: REJECTED", ha='center', va='center', fontsize=10.5, fontweight='bold', color='#991B1B')
    ax.text(82, 51, "Terminal claim state\nItem -> OPEN (if no active claims)\nReview notes recorded", 
            ha='center', va='center', fontsize=8, color='#7F1D1D')

    # State: APPROVED
    b_appr = patches.FancyBboxPatch((35, 48), 30, 12, boxstyle="round,pad=0.8", 
                                    facecolor='#DCFCE7', edgecolor='#16A34A', linewidth=2)
    ax.add_patch(b_appr)
    ax.text(50, 55.5, "CLAIM: APPROVED", ha='center', va='center', fontsize=11, fontweight='bold', color='#14532D')
    ax.text(50, 51, "Staff approval confirmed\nSingle Approved Claim Rule enforced\nItem remains CLAIM_PENDING", 
            ha='center', va='center', fontsize=8.5, color='#166534')

    # State: COMPLETED
    b_comp = patches.FancyBboxPatch((30, 14), 40, 16, boxstyle="round,pad=0.8", 
                                    facecolor='#EDE9FE', edgecolor='#7C3AED', linewidth=2.5)
    ax.add_patch(b_comp)
    ax.text(50, 24.5, "CLAIM: COMPLETED (HANDOVER COMPLETE)", ha='center', va='center', fontsize=11, fontweight='bold', color='#5B21B6')
    ax.text(50, 18, "- Physical item returned to verified owner\n- FoundItem -> RETURNED\n- Linked LostItem -> RETURNED\n- Competing pending claims -> Auto-REJECTED", 
            ha='center', va='center', fontsize=8.5, color='#4C1D95')

    # Arrows between states
    # Pending to Info
    ax.annotate('Staff requests info', xy=(21, 61), xytext=(35, 79),
                arrowprops=dict(facecolor='#0284C7', edgecolor='#0284C7', width=1.5, headwidth=6),
                fontsize=8, color='#0369A1', fontweight='bold')

    # Info back to Pending
    ax.annotate('Claimant updates info', xy=(40, 75), xytext=(25, 61),
                arrowprops=dict(facecolor='#0284C7', edgecolor='#0284C7', width=1.2, headwidth=5),
                fontsize=7.5, color='#0369A1')

    # Pending to Approved
    ax.annotate('Staff approves claim', xy=(50, 61), xytext=(50, 75),
                arrowprops=dict(facecolor='#16A34A', edgecolor='#16A34A', width=1.5, headwidth=6),
                fontsize=8, color='#15803D', fontweight='bold')

    # Pending to Rejected
    ax.annotate('Staff rejects claim', xy=(79, 61), xytext=(65, 79),
                arrowprops=dict(facecolor='#DC2626', edgecolor='#DC2626', width=1.5, headwidth=6),
                fontsize=8, color='#B91C1C', fontweight='bold')

    # Approved to Completed
    ax.annotate('Staff completes handover in person', xy=(50, 31), xytext=(50, 47),
                arrowprops=dict(facecolor='#7C3AED', edgecolor='#7C3AED', width=2, headwidth=7),
                fontsize=8.5, color='#6D28D9', fontweight='bold')

    # Completed to Rejected competing claims
    ax.annotate('Cascade reject all other claims', xy=(82, 47), xytext=(71, 25),
                arrowprops=dict(facecolor='#DC2626', edgecolor='#DC2626', width=1.5, headwidth=6, connectionstyle="arc3,rad=-0.3"),
                fontsize=8, color='#DC2626', fontweight='bold')

    plt.tight_layout()
    plt.savefig('doc_assets/claim_lifecycle_state_machine.png', dpi=300, facecolor='#F8FAFC')
    plt.close()
    print("Saved claim_lifecycle_state_machine.png")

def create_matching_pipeline_diagram():
    fig, ax = plt.subplots(figsize=(12, 7.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')
    fig.patch.set_facecolor('#F8FAFC')
    ax.set_facecolor('#F8FAFC')

    ax.text(50, 96, "LostLink Explainable 100-Point Matching Engine Pipeline", 
            ha='center', va='center', fontsize=15, fontweight='bold', color='#0F172A')

    # Input Box
    b_in = patches.FancyBboxPatch((25, 84), 50, 7.5, boxstyle="round,pad=0.6", 
                                  facecolor='#E2E8F0', edgecolor='#475569', linewidth=1.5)
    ax.add_patch(b_in)
    ax.text(50, 89, "INPUT CANDIDATE PAIR", ha='center', va='center', fontsize=10.5, fontweight='bold', color='#1E293B')
    ax.text(50, 86, "LostItem (Status: OPEN)   vs   FoundItem (Status: OPEN / CLAIM_PENDING)", 
            ha='center', va='center', fontsize=9, color='#334155')

    ax.annotate('', xy=(50, 77), xytext=(50, 83),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', width=1.5, headwidth=6))

    # 5 Scoring Weights Blocks
    weights = [
        ("1. Category Match", "Max: 30 Pts", "Exact Enum Equality\n(e.g., ELECTRONICS)", "#DBEAFE", "#1D4ED8", 4),
        ("2. Keyword Overlap", "Max: 25 Pts", "Name & Description Tokens\nFiltered Stop Words", "#FEF3C7", "#B45309", 23),
        ("3. Color Match", "Max: 15 Pts", "Exact = 15 pts\nSubstring = 10 pts", "#DCFCE7", "#15803D", 42),
        ("4. Location Match", "Max: 20 Pts", "Exact = 20, Substring = 15\nToken proximity = up to 20", "#EDE9FE", "#6D28D9", 61),
        ("5. Date Proximity", "Max: 10 Pts", "Linear decay window\n<= 14 days apart", "#FEE2E2", "#B91C1C", 80)
    ]

    for title, pts, desc, bg, border, xpos in weights:
        b_w = patches.FancyBboxPatch((xpos, 52), 16.5, 22, boxstyle="round,pad=0.5", 
                                     facecolor=bg, edgecolor=border, linewidth=1.5)
        ax.add_patch(b_w)
        ax.text(xpos + 8.25, 70, title, ha='center', va='center', fontsize=8.5, fontweight='bold', color=border)
        ax.text(xpos + 8.25, 66, pts, ha='center', va='center', fontsize=8.5, fontweight='bold', color='#0F172A')
        ax.text(xpos + 8.25, 59, desc, ha='center', va='center', fontsize=7.5, color='#334155')
        ax.annotate('', xy=(xpos + 8.25, 45), xytext=(xpos + 8.25, 51.5),
                    arrowprops=dict(facecolor=border, edgecolor=border, width=1.2, headwidth=5))

    # Summation & Reason Generator
    b_sum = patches.FancyBboxPatch((15, 30), 70, 13, boxstyle="round,pad=0.6", 
                                   facecolor='#FEF9C3', edgecolor='#CA8A04', linewidth=2)
    ax.add_patch(b_sum)
    ax.text(50, 39, "TOTAL SCORE (0 - 100) & EXPLANATION BUILDER", ha='center', va='center', fontsize=11, fontweight='bold', color='#854D0E')
    ax.text(50, 33.5, "Total Score = Category(30) + Keywords(25) + Color(15) + Location(20) + Date(10)\nConstructs List<String> reasons: ['Category matched (+30)', 'Color matched (+15)', ...]\nTransparent Disclosure: 'Similarity Score only; NEVER proof of ownership'", 
            ha='center', va='center', fontsize=8.5, color='#713F12')

    ax.annotate('', xy=(50, 22), xytext=(50, 29.5),
                arrowprops=dict(facecolor='#854D0E', edgecolor='#854D0E', width=1.5, headwidth=6))

    # Threshold, Tie-Break, Top 3 Output
    b_out = patches.FancyBboxPatch((10, 5), 80, 15, boxstyle="round,pad=0.6", 
                                   facecolor='#F1F5F9', edgecolor='#334155', linewidth=2)
    ax.add_patch(b_out)
    ax.text(50, 16.5, "CANDIDATE FILTERING, RANKING & BOUNDED ARRAY OUTPUT", ha='center', va='center', fontsize=11, fontweight='bold', color='#0F172A')
    ax.text(50, 10, "1. Eligibility Filter: Score >= 30 (MIN_MATCH_SCORE) & Found Item not RETURNED\n2. Deterministic Tie-Breaker: Score DESC -> Found Date DESC -> Item ID ASC\n3. Bounded Output: MatchResult[] array strictly capped at TOP 3 candidates", 
            ha='center', va='center', fontsize=8.5, color='#334155')

    plt.tight_layout()
    plt.savefig('doc_assets/matching_scoring_pipeline.png', dpi=300, facecolor='#F8FAFC')
    plt.close()
    print("Saved matching_scoring_pipeline.png")

if __name__ == '__main__':
    create_architecture_diagram()
    create_workflow_diagram()
    create_claim_lifecycle_diagram()
    create_matching_pipeline_diagram()
    print("All diagrams generated successfully!")
