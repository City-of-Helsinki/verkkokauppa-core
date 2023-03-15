curl -X 'POST' \
  'http://localhost:8084/v1/order/instantPurchase' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{"products":[{"productId":"306ab20a-6b30-3ce3-95e8-fef818e6c30e","unit":"pcs","quantity":1}],"language":"fi","namespace":"venepaikat","user":"1d80ea69-70dc-4191-befa-ede62bd26c31"}'