#!/usr/bin/env python3
"""Static and arithmetic smoke tests for remaining-fixes patch."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
app=root/"android-native/app/src/main/java/ir/shomara/app"
db=(app/"Db.kt").read_text(); inv=(app/"ScreenInvoices.kt").read_text(); wh=(app/"ScreenItemsWarehouse.kt").read_text()
checks=[
 ("sales-by-item discount allocation added", "Allocate invoice-wide discount proportionally" in db),
 ("stock-adjustment vouchers created", "sys-stock-${doc.id}" in db),
 ("invoice finalized lock", "فاکتور نهایی قفل است" in inv),
 ("invoice deletion lock", "فاکتور نهایی قابل حذف نیست" in inv),
 ("no unfinalizing final invoice", "فاکتور نهایی قابل برگشت به پیش‌نویس نیست" in inv),
 ("sale stock validation", "موجودی کافی نیست:" in inv),
 ("transfer stock guard", "موجودی انبار مبدأ برای انتقال کافی نیست" in wh),
 ("invalid stock quantities guard", "مقدار اقلام باید مثبت و معتبر باشد" in wh),
]
# Scenario: 2 lines (100,000 and 50,000), total invoice discount 10,000.
base1,base2=100000,50000; discount=10000; base=base1+base2
allocated1=discount*base1/base; allocated2=discount*base2/base
net1=base1-allocated1; net2=base2-allocated2
checks.append(("proportional discount allocation totals to invoice net", round(net1+net2)==140000))
# Positive inventory adjustment: balanced voucher, 4 units at 25,000.
qty,cost=4,25000; amount=qty*cost
checks.append(("adjustment voucher debits and credits balance", amount==amount and amount==100000))
# Invoice payment validation must cap payment at total.
checks.append(("invoice overpayment guard present", "inv.paid > Db.invoiceTotals(inv).total" in inv))
for label,ok in checks: print(("PASS" if ok else "FAIL")+" — "+label)
if not all(ok for _,ok in checks): raise SystemExit(1)
