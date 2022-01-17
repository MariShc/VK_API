#language:ru
@test2

Функционал: Редактирование профиля VK
  - Получить всю информацию о текущем профиле
  - Заполнить недостающую информацию
  - Сменить фото профиля на любую другу

  Сценарий: Выполнение GET запроса для получения информации о профиле
    * создать запрос
      | method | path                   |
      | GET    | account.getProfileInfo |
    * добавить параметры token и version
    * отправить запрос
    * получение ответа и запись тела ответа в файл
    * статус код 200
    * создать запрос
      | method | path                    |
      | GET    | account.saveProfileInfo |
    * добавить параметры token и version
    * добавить query параметры
      | maiden_name | Андриянова |
      | home_town   | Саратов    |
    * отправить запрос
    * статус код 200
    * создать запрос
      | method | path                             |
      | GET    | photos.getOwnerPhotoUploadServer |
    * добавить параметры token и version
    * добавить query параметры
      | owner_id | 38575187 |
    * отправить запрос
    * статус код 200
    * извлечь данные
      | upload_url | $.response.upload_url |
    * создать запрос
      | method | url           | body      |
      | POST   | ${upload_url} | photo.jpg |
    * добавить параметры token и version
    * добавить header
      | Content-Type | multipart/form-data |
    * отправить запрос
    * статус код 200
#    * извлечь данные
#      | server | $.server |
#      | photo  | $.photo  |
#      | hash   | $.hash   |
#    * создать запрос
#      | method | path                  |
#      | GET    | photos.saveOwnerPhoto |
#    * добавить параметры token и version
#    * добавить query параметры
#      | server | ${server} |
#      | photo  | ${photo}  |
#      | hash   | ${hash}   |
#    * отправить запрос
#    * статус код 200