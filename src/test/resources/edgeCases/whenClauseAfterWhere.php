<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->where('goods_code', 1)
    ->when(1, function (\Hyperf\Database\Model\Builder $query) {
        $query->where('<caret>', 1);
    })
    ->get();
