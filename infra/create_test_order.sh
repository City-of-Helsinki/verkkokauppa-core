curl -X 'POST' \
  'http://localhost:8084/v1/order/instantPurchase' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{"products":[{"productId":"b86337e8-68a0-3599-a18b-754ffae53f5a","unit":"pcs","quantity":1}],"language":"fi","namespace":"venepaikat","user":"1d80ea69-70dc-4191-befa-ede62bd26c31"}'