#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
base=ROOT/'android-native/app/src/main/java/ir/shomara/app'
partner=(base/'ScreenPartnersCashChecks.kt').read_text()
invoice=(base/'ScreenInvoices.kt').read_text()
stock=(base/'ScreenItemsWarehouse.kt').read_text()
voucher=(base/'ScreenLedgerReports.kt').read_text()
model=(base/'Models.kt').read_text()
db=(base/'Db.kt').read_text()
workflow=(ROOT/'.github/workflows/android-apk.yml').read_text()
build=(ROOT/'scripts/build_apk_native.sh').read_text()
checks={
 'Partner national ID format and checksum validation':'کد ملی/شناسه ملی باید ۱۰ تا ۱۲ رقم باشد' in partner and 'val valid = if (sum < 2) check == sum else check == 11 - sum' in partner,
 'Partner ID duplicate check normalization':'normalizeId(it.nationalId)' in partner,
 'Partner extra form fields are part of model':'val email: String = ""' in model and 'val creditLimit: Long = 0' in model,
 'Partner fields persist and legacy JSON has defaults':'put("email", p.email)' in db and 's(o, "email")' in db and 'o.optLong("creditLimit", 0)' in db,
 'Payments require a valid partner':'طرف‌حساب را انتخاب کنید' in partner,
 'Payments validate partner role':'برای دریافت، مشتری یا طرف‌حساب مشترک' in partner and 'برای پرداخت، فروشنده یا طرف‌حساب مشترک' in partner,
 'Checks validate chronology':'سررسید چک نمی‌تواند قبل از تاریخ صدور/دریافت باشد' in partner,
 'Invoice line discount cannot exceed unit price':'it.discount > it.price.toDouble()' in invoice,
 'Invoice VAT range validated':'inv.vatRate !in 0L..100L' in invoice,
 'Credit limit blocks sales finalization over cap':'partner.creditLimit > 0L' in invoice and 'Db.partnerBalance(Db.db, partner.id).net' in invoice,
 'Invoice partner role validated':'در فاکتور خرید باید فروشنده انتخاب شود' in invoice and 'در فاکتور فروش باید مشتری انتخاب شود' in invoice,
 'Invoice missing item references blocked':'یکی از کالاهای فاکتور دیگر در فهرست کالاها وجود ندارد' in invoice,
 'Supplier invoice reference is optional and persisted':'val supplierInvoiceNo: String = ""' in model and 'put("supplierInvoiceNo", i.supplierInvoiceNo)' in db and 's(o, "supplierInvoiceNo")' in db,
 'Stock documents prevent duplicate items':'doc.lines.map { it.itemId }.distinct().size != doc.lines.size' in stock,
 'Stock documents require valid warehouse':'انبار معتبر انتخاب کنید' in stock,
 'Manual vouchers require two lines':'سند حسابداری حداقل باید دو ردیف داشته باشد' in voucher,
 'Manual voucher rows validate debit/credit exclusivity':'هر ردیف باید فقط یک مبلغ مثبت' in voucher,
 'GitHub workflow triggers manual and main/master pushes':'workflow_dispatch:' in workflow and 'branches: [main, master]' in workflow,
 'Build script is repository-relative':'ROOT=${SHOMARA_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}' in build,
}
for label,ok in checks.items(): print(('PASS' if ok else 'FAIL')+' — '+label)
print(f'{sum(checks.values())}/{len(checks)} static form/build checks passed')
raise SystemExit(0 if all(checks.values()) else 1)
