<?php

namespace App {
class Goods extends \Hyperf\Database\Model\Model
{
    protected $table = 'users';
}

class GoodsData
{
    protected Goods $model;

    public function info(int $id, array $field = ['*'])
    {
        return $this->model::query()
            ->select($field)
            ->where('<caret>')
            ->first();
    }
}
}
